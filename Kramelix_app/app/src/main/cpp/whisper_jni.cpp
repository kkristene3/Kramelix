/*
 * AMY'S NOTE: This file connects our Java code to C++ (the Whisper.cpp repo) like a bridge,
 * using the Java Native Interface (JNI). We later call the two main functions below to run
 * the native code and return the results.
 */

#include <jni.h>
#include <android/log.h>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>
#include <algorithm>

#include "whisper.h" // Whisper.cpp public API

// Adding log macros for convenience ("whisper_jni" tag before msgs)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO , "whisper_jni", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "whisper_jni", __VA_ARGS__)

/**
 * Global pointer to loaded Whisper context -> to be used in model initialization & reused for every transcription
 */
static whisper_context *gCtx = nullptr;

// -------------------- WAV loader & pre-processing --------------------
/**
 * This helper function converts a PCM WAV to mono float @ 16 kHz (what Whisper expects).
 *
 * It accepts a 16/24/32-bit integer PCM, mono or stereo (or more channels).
 * - If multi-channel: averages channels to mono (simple mean avg. helps preserve volume).
 * - If sample rate != 16000: linear resample to 16 kHz.
 *
 * @param path the absolute path to the WAV file on disk
 * @param pcmf32Out an array filled with normalized mono samples in range [-1, 1] at 16 kHz
 * @return true on success; false on failure (WAV is unreadable/unsupported)
 */
static bool readWavTo16KMonoF32(const char *path, std::vector<float> &pcmf32Out) {

    // PROCESS: opening the file in binary mode
    FILE *f = fopen(path, "rb");

    if (!f) { // error-handling

        // OUTPUT:
        LOGE("Audio file open failed: %s", path);
        return false;

    }

    // PROCESS: checking for expected start "RIFF" and end "WAVE" in header
    char riff[4], wave[4];
    uint32_t riffSize;

    if (fread(riff, 1, 4, f) != 4
        || fread(&riffSize, 4, 1, f) != 1
        || fread(wave, 1, 4, f) != 4
        || memcmp(riff, "RIFF", 4) != 0
        || memcmp(wave, "WAVE", 4) != 0) { // invalid header -> therefore, not a standard WAV

        // OUTPUT:
        LOGE("No RIFF/WAVE found in the WAV header!");
        fclose(f); // closing file
        return false;

    }

    // VARIABLE DECLARATION: setting labels for format & data chunk positions
    uint16_t audioFmt = 0, numCh = 0, bits = 0, blockAlign = 0;
    uint32_t srcSr = 0, byteRate = 0, dataBytes = 0;
    long dataPos = 0;

    // PROCESS: locating fmt & data chunks
    while (!feof(f)) {

        char id[4];
        uint32_t sz = 0;

        // PROCESS: reading 4-byte chunk ID + 4-byte chunk size; if impossible, we've hit EOF or a corrupt file
        if (fread(id, 1, 4, f) != 4 || fread(&sz, 4, 1, f) != 1) break;

        if (!memcmp(id, "fmt ", 4)) { // found expected format chunk

            // PROCESS: checking that PCM fmt chunk is >= 16 bytes
            if (sz < 16) { // skipping weird/short chunk in scan
                fseek(f, sz, SEEK_CUR);
                continue;
            }

            // PROCESS: reading format fields
            fread(&audioFmt, 2, 1, f); // 1 = PCM
            fread(&numCh, 2, 1, f); // channels (1 = mono, 2 = stereo, etc.)
            fread(&srcSr, 4, 1, f); // sample rate (e.g. 16000, 44100, 48000)
            fread(&byteRate, 4, 1, f); // byte rate not used, but must be read to ensure correct indexing
            fread(&blockAlign, 2, 1, f); // block align not used, but must be read to ensure correct indexing
            fread(&bits, 2, 1, f); // bits per sample (16/24/32)

            // PROCESS: skipping any extra fmt bytes (unnecessary for basic PCM read)
            if (sz > 16) fseek(f, sz - 16, SEEK_CUR);

        } else if (!memcmp(id, "data", 4)) { // found expect data chunk

            // PROCESS: saving audio data start & # of bytes for later reads
            dataBytes = sz;
            dataPos = ftell(f);
            fseek(f, sz, SEEK_CUR); // skipping data for now (will seek back here later)

        } else { // unexpected/unnecessary extra chunk

            // PROCESS: skipping others (LIST/fact/etc.) bc not needed for PCM decoding
            fseek(f, sz, SEEK_CUR);

        }

    }

    // PROCESS: checking for a real data chunk
    if (dataPos == 0 || dataBytes == 0) { // file doesn't contain readable audio sample

        // OUTPUT:
        LOGE("No audio data chunk readable!");
        fclose(f); // closing file
        return false;

    }

    // PROCESS: checking that format is an int PCM with supported bit depth
    if (audioFmt != 1 || (bits != 16 && bits != 24 && bits != 32)) { // unsupported

        // OUTPUT:
        LOGE("Unsupported WAV format: fmt = %u, bits = %u", audioFmt, bits);
        fclose(f); // closing file
        return false;

    }

    // PROCESS: checking for at least one audio channel
    if (numCh < 1) { // probably corrupted or smth

        // OUTPUT:
        LOGE("Audio channels found < 1!");
        fclose(f);
        return false;

    }

    // PROCESS: reading into mono float samples in [-1, 1] (expected by Whisper); for stereo, we avg. to mono
    std::vector<float> mono;
    mono.reserve(dataBytes / (bits / 8)); // rough upper bound
    fseek(f, dataPos, SEEK_SET); // jumping to start of audio data

    // PROCESS: computing how many frames (samples per channel) are present
    const size_t frames = dataBytes / (numCh * (bits / 8));

    for (size_t i = 0; i < frames; ++i) {

        double acc = 0.0; // accumulator across channels for averaging to mono

        // AMY'S NOTE: refer to https://www.mikeash.com/pyblog/friday-qa-2012-10-12-obtaining-and-interpreting-audio-data.html for common normalization processes
        for (int ch = 0; ch < numCh; ++ch) {

            if (bits == 16) { // expected & most common on mobile

                int16_t s;
                fread(&s, 2, 1, f);
                acc += (double) s / 32768.0; // normalizing

            } else if (bits == 24) { // expected, but higher precision

                unsigned char b[3];
                fread(b, 1, 3, f);
                auto v = (int32_t) ((b[0]) | (b[1] << 8) | (b[2] << 16));

                if (v & 0x800000) v |= ~0xFFFFFF; // sign-extend bit 23
                acc += (double) v / 8388608.0; // normalizing

            } else { // rare for phone recordings

                int32_t s;
                fread(&s, 4, 1, f);
                acc += (double) s / 2147483648.0; // normalizing

            }

        }

        // PROCESS: averaging across channels to mono
        mono.push_back((float) (acc / numCh));

    }

    fclose(f); // closing file after handling

    // PROCESS: checking if sample rate matches target
    if (srcSr == 16000) { // no resample needed

        pcmf32Out.swap(mono); // sending mono to output
        return true;

    } else if (srcSr == 0) { // invalid/corrupt

        // OUTPUT:
        LOGE("invalid sample rate 0");
        return false;

    }

    // PROCESS: linear resample to 16 kHz
    const double ratio = 16000.0 / (double) srcSr; // aka how much to stretch/shrink time
    const double scaled = static_cast<double>(mono.size()) * ratio;
    const size_t outN = static_cast<size_t>(std::max(1.0, scaled));
    pcmf32Out.resize(outN);

    // PROCESS: resampling
    for (size_t i = 0; i < outN; ++i) {

        double srcPos = (double) i / ratio; // fractional index (a point in time) within larger input (full src timeline)
        auto i0 = (size_t) srcPos; // left neighbour
        size_t i1 = std::min(mono.size() - 1, i0 + 1); // right neighbour (clamped)
        double time = srcPos - static_cast<double>(i0); // fractional part 0 ... 1

        // PROCESS: linear interpolation btwn neighbours to give approx. of the audio signal at srcPos
        auto signal = (float) ((1.0 - time) * mono[i0] + time * mono[i1]);
        pcmf32Out[i] = signal;

    }

    // OUTPUT: logging resampled stats for debugging (size in samples, not bytes)
    LOGI("Resampled %zu -> %zu (sr %u -> 16000)", mono.size(), pcmf32Out.size(), srcSr);
    return true;
}

// -------------------- JNI exports --------------------
/*
 * AMY'S NOTE: These functions are the bridge endpoints of our Java calls.
 * Apparently, their names MUST match our Java pkg/class/method exactly,
 * so even tho it's ugly, we end up with smth like:
 *
 * package com.example.kramelix.ml.whisper;
 * public class Whisper {
 *  static { System.loadLibrary("whisper_jni"); }
 *  public static native boolean initModel(String modelPath);
 *  public static native String transcribeWav(String wavPath);
 * }
 * in Java, becoming
 *
 * Java_com_example_kramelix_ml_whisper_Whisper_initModel &
 * Java_com_example_kramelix_ml_whisper_Whisper_transcribeWav
 * in C++
 */

/**
 * This function initializes Whisper once per app lifecycle (Whisper's `initModel` function).
 *
 * @param env the JNI environment (needed for converting Java strings, creating return vals., etc.)
 * @param jclass the Java class object (unused)
 * @param jModelPath the Java String pointing to the model file
 * @return JNI_TRUE on success (model loaded); JNI_FALSE on failure (bad path/corrupt file/low memory)
 */
extern "C" JNIEXPORT jboolean

JNICALL
Java_com_example_kramelix_whisperjni_Whisper_initModel(JNIEnv *env, jclass, jstring jModelPath) {

    // VARIABLE DECLARATION: converting Java model path to C string
    const char *modelPath = env->GetStringUTFChars(jModelPath, nullptr);

    // PROCESS: checking if a model already exists
    if (gCtx) { // exists

        whisper_free(gCtx); // freeing model to avoid leaks
        gCtx = nullptr;

    }

    whisper_context_params params = whisper_context_default_params();
    gCtx = whisper_init_from_file_with_params(modelPath, params); // creating a new Whisper context from the model file on disk
    env->ReleaseStringUTFChars(jModelPath, modelPath); // releasing the pinned Java string

    // PROCESS: checking for load success
    if (!gCtx) { // fail

        // OUTPUT:
        LOGE("`whisper_init_from_file` failed!");
        return JNI_FALSE;

    }

    // OUTPUT: otherwise, success
    LOGI("Whisper model loaded!");
    return JNI_TRUE;

}

/**
 * This function runs the end-to-end transcription for a WAV file (Whisper's `transcribeWav` function).
 *
 * @param env the JNI environment (needed for converting Java strings, creating return vals., etc.)
 * @param jclass the Java class object (unused)
 * @param jWavPath the Java string pointing to the WAV audio file
 * @return a new Java String with the transcript text on success; a bracketed error message on failure
 */
extern "C" JNIEXPORT jstring

JNICALL
Java_com_example_kramelix_whisperjni_Whisper_transcribeWav(JNIEnv *env, jclass, jstring jWavPath) {

    // PROCESS: checking that model's been initialized
    if (!gCtx) {

        // OUTPUT:
        return env->NewStringUTF("[Whisper not initialized]");

    }

    // VARIABLE DECLARATION: converting Java path to C string
    const char *wavPath = env->GetStringUTFChars(jWavPath, nullptr);

    // PROCESS: converting WAV to mono float @ 16 kHz
    std::vector<float> pcmf32;

    // Calling our helper function:
    if (!readWavTo16KMonoF32(wavPath, pcmf32)) { // error-handling

        env->ReleaseStringUTFChars(jWavPath, wavPath); // release before returning

        // OUTPUT:
        return env->NewStringUTF("[Failed to read WAV (or resample)]");

    }

    // VARIABLE DECLARATION: Whisper inference params.
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_progress = false; // no spammy logs
    params.print_realtime = false; // don't print while running
    params.print_timestamps = false; // only text
    params.language = "en"; // English-only (use "auto" or set others if we need them later)
    params.translate = false; // no translation for now (speech -> same language)

    // FIXME OPTIMIZE: possible speed tweaks for later?
    // params.n_threads = std::max(1, (int) sysconf(_SC_NPROCESSORS_ONLN)); // using all cores
    // params.speed_up = true; // quality tradeoff for faster decoding

    // PROCESS: running the model on our float samples
    int rc = whisper_full(gCtx, params, pcmf32.data(), (int) pcmf32.size());

    env->ReleaseStringUTFChars(jWavPath, wavPath); // releasing the pinned Java string

    // PROCESS: checking for failed decodes
    if (rc != 0) {

        // OUTPUT:
        return env->NewStringUTF("[Transcription failed]");

    }

    // PROCESS: gathering all segment texts
    std::string out;
    int n = whisper_full_n_segments(gCtx);

    for (int i = 0; i < n; ++i) {
        out += whisper_full_get_segment_text(gCtx, i); // appending each segment to output
    }

    // OUTPUT: returning new Java String
    return env->NewStringUTF(out.c_str());

}
