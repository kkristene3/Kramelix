#include <jni.h>
#include <android/log.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>
#include <algorithm>

#include "whisper.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO , "whisper_jni", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "whisper_jni", __VA_ARGS__)

static whisper_context* g_ctx = nullptr;

// --- Robust WAV reader: reads PCM (16/24/32) mono/stereo at any SR, mixes to mono,
//     and resamples to 16k using simple linear interpolation.
static bool read_wav_to_16k_mono_f32(const char* path, std::vector<float>& pcmf32_out) {
    FILE* f = fopen(path, "rb");
    if (!f) { LOGE("open fail: %s", path); return false; }

    char riff[4]; uint32_t riff_size; char wave[4];
    if (fread(riff,1,4,f)!=4 || fread(&riff_size,4,1,f)!=1 || fread(wave,1,4,f)!=4
        || memcmp(riff,"RIFF",4) || memcmp(wave,"WAVE",4)) {
        LOGE("Not RIFF/WAVE");
        fclose(f); return false;
    }

    uint16_t audio_fmt=0, num_ch=0, bits=0; uint32_t src_sr=0;
    uint32_t data_bytes=0; long data_pos=0;

    while (!feof(f)) {
        char id[4]; uint32_t sz=0;
        if (fread(id,1,4,f)!=4 || fread(&sz,4,1,f)!=1) break;
        if (!memcmp(id,"fmt ",4)) {
            if (sz < 16) { fseek(f, sz, SEEK_CUR); continue; }
            uint16_t block_align=0; uint32_t byte_rate=0;
            fread(&audio_fmt,     2,1,f);
            fread(&num_ch,        2,1,f);
            fread(&src_sr,        4,1,f);
            fread(&byte_rate,     4,1,f);
            fread(&block_align,   2,1,f);
            fread(&bits,          2,1,f);
            if (sz > 16) fseek(f, sz - 16, SEEK_CUR);
        } else if (!memcmp(id,"data",4)) {
            data_bytes = sz;
            data_pos = ftell(f);
            fseek(f, sz, SEEK_CUR);
        } else {
            fseek(f, sz, SEEK_CUR);
        }
    }

    if (data_pos == 0 || data_bytes == 0) { LOGE("no data chunk"); fclose(f); return false; }
    if (audio_fmt != 1 || (bits != 16 && bits != 24 && bits != 32)) {
        LOGE("Unsupported WAV format: fmt=%u bits=%u", audio_fmt, bits);
        fclose(f); return false;
    }
    if (num_ch < 1) { LOGE("channels < 1"); fclose(f); return false; }

    std::vector<float> mono;
    mono.reserve(data_bytes / (bits/8));
    fseek(f, data_pos, SEEK_SET);

    const size_t frames = data_bytes / (num_ch * (bits/8));
    for (size_t i = 0; i < frames; ++i) {
        double acc = 0.0;
        for (int ch = 0; ch < num_ch; ++ch) {
            if (bits == 16) {
                int16_t s; fread(&s, 2, 1, f);
                acc += (double)s / 32768.0;
            } else if (bits == 24) {
                unsigned char b[3]; fread(b,1,3,f);
                int32_t v = (int32_t)((b[0]) | (b[1]<<8) | (b[2]<<16));
                if (v & 0x800000) v |= ~0xFFFFFF; // sign extend
                acc += (double)v / 8388608.0;
            } else { // 32-bit signed PCM
                int32_t s; fread(&s, 4, 1, f);
                acc += (double)s / 2147483648.0;
            }
        }
        mono.push_back((float)(acc / num_ch)); // average to mono
    }
    fclose(f);

    if (src_sr == 16000) { pcmf32_out.swap(mono); return true; }
    if (src_sr == 0) { LOGE("invalid sample rate 0"); return false; }

    // Resample to 16k linear
    const double ratio = 16000.0 / (double)src_sr;
    const size_t outN = (size_t)std::max<size_t>(1, (size_t)(mono.size() * ratio));
    pcmf32_out.resize(outN);

    for (size_t i = 0; i < outN; ++i) {
        double srcPos = (double)i / ratio;
        size_t i0 = (size_t)srcPos;
        size_t i1 = std::min(mono.size()-1, i0+1);
        double t = srcPos - i0;
        float s = (float)((1.0 - t) * mono[i0] + t * mono[i1]);
        pcmf32_out[i] = s;
    }
    LOGI("Resampled %zu -> %zu (sr %u -> 16000)", mono.size(), pcmf32_out.size(), src_sr);
    return true;
}

// -------- JNI exports (match: package com.example.kramelix; class Whisper; static natives) --------

extern "C" JNIEXPORT jboolean
Java_com_example_kramelix_Whisper_initModel(JNIEnv* env, jclass, jstring jModelPath) {
    const char* modelPath = env->GetStringUTFChars(jModelPath, nullptr);
    if (g_ctx) {
        whisper_free(g_ctx);
        g_ctx = nullptr;
    }

    g_ctx = whisper_init_from_file(modelPath);
    env->ReleaseStringUTFChars(jModelPath, modelPath);

    if (!g_ctx) {
        LOGE("whisper_init_from_file failed");
        return JNI_FALSE;
    }
    LOGI("Whisper model loaded");
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_kramelix_Whisper_transcribeWav(JNIEnv* env, jclass, jstring jWavPath) {
    if (!g_ctx) {
        return env->NewStringUTF("[Whisper not initialized]");
    }

    const char* wavPath = env->GetStringUTFChars(jWavPath, nullptr);

    std::vector<float> pcmf32;
    if (!read_wav_to_16k_mono_f32(wavPath, pcmf32)) {
        env->ReleaseStringUTFChars(jWavPath, wavPath);
        return env->NewStringUTF("[Failed to read WAV (or resample)]");
    }

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_progress   = false;
    params.print_realtime   = false;
    params.print_timestamps = false;
    params.language         = "en";
    params.translate        = false;

    int rc = whisper_full(g_ctx, params, pcmf32.data(), (int)pcmf32.size());
    env->ReleaseStringUTFChars(jWavPath, wavPath);

    if (rc != 0) return env->NewStringUTF("[Transcription failed]");

    std::string out;
    int n = whisper_full_n_segments(g_ctx);
    for (int i = 0; i < n; ++i) out += whisper_full_get_segment_text(g_ctx, i);
    return env->NewStringUTF(out.c_str());
}
