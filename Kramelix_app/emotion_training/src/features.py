"""
This module extracts robust audio features using librosa, including:
- MFCC
- MFCC Δ (1st-order derivative)
- MFCC ΔΔ (2nd-order derivative)
- Pitch (F0) + pitch confidence
- Energy features
- Spectral shape features

Author: Amy Huang
Since: 1.0
"""

import librosa
import numpy as np


def extract_mfcc(path: str, mfcc_count: int = 40) -> np.ndarray:
    """
    @brief
        Loads a WAV file & computes an audio feature representation.

    @details
        The MFCC extraction pipeline includes:
        1. Loading audio at a fixed sampling rate (16 kHz)
        2. Applying pre-emphasis to boost high-frequency cues
        3. Trimming leading/trailing silence
        4. Normalizing waveform amplitude
        5. Extracting MFCC features (mfcc, delta, delta-delta)
        6. Extracting emotion-relevant features:
            - F0 pitch
            - RMS energy
            - Zero-crossing rate
            - Spectral centroid, bandwidth, contrast, rolloff
        7. Time-average everything into a stable vector

        Total feature length = (mfcc_count * 3) + extra_scalar_features

    @param
        path: str
            Absolute or relative path to the WAV file.

    @param
        mfcc_count: int
            # of MFCC coefficients (dimensions) to compute.

    @return
        np.ndarray (float32)
            1D feature vector representing the audio clip.

    @throws
        Exception
            Throws exceptions related to file loading, invalid audio structure, or numerical failures.
            These are caught by preprocess.py later on.

        ValueError
            Raised if the audio file is empty after loading.
    """

    # VARIABLE DECLARATION: loading waveform at 16 kHz
    # (AMY'S NOTE: I think this is a common ML standard?)
    y, sr = librosa.load(path, sr=16000)  # y = waveform, sr = sampling rate

    # PROCESS: skipping empty audio
    if y.size == 0:
        raise ValueError(f"Empty audio file: {path}")

    # PROCESS: boosting high frequencies (pre-emphasis)
    y = np.append(y[0], y[1:] - 0.97 * y[:-1])

    # PROCESS: trimming leading/trailing silence
    y, _ = librosa.effects.trim(y, top_db=40)

    # PROCESS: normalizing amplitude
    if y.std() > 0:
        y = (y - np.mean(y)) / np.std(y)

    # VARIABLE DECLARATION: windowing parameters
    n_fft = int(sr * 0.025)  # 25 ms window
    hop_length = int(sr * 0.010)  # 10 ms stride

    # PROCESS: computing MFCC feature (voice timbre)
    mfcc = librosa.feature.mfcc(
        y=y,
        sr=sr,
        n_mfcc=mfcc_count,
        n_fft=n_fft,
        hop_length=hop_length,
    )

    # PROCESS: computing 1st & 2nd derivatives
    delta = librosa.feature.delta(mfcc)
    delta2 = librosa.feature.delta(mfcc, order=2)

    # PROCESS: stacking & time-averaging MFCC + deltas
    mfcc_stack = np.vstack([mfcc, delta, delta2])  # (3*mfcc_count, frames)
    mfcc_vector = np.mean(mfcc_stack, axis=1)  # becomes (3*mfcc_count,)

    # PROCESS: computing pitch feature (F0)
    try:

        f0, voiced_flag, voiced_prob = librosa.pyin(
            y,
            fmin=librosa.note_to_hz("C2"),
            fmax=librosa.note_to_hz("C7"),
            sr=sr,
        )

        f0_mean = (
            np.nanmean(f0) if np.any(~np.isnan(f0)) else 0.0
        )  # removing NaN values (unvoiced frames)
        f0_conf = np.mean(voiced_prob) if voiced_prob is not None else 0.0

    except Exception as e:  # error-handling

        # OUTPUT:
        print(f"[WARNING] Failed to compute pitch features: {e}")

        f0_mean = 0.0  # resetting to defaults
        f0_conf = 0.0

    # PROCESS: computing energy features (RMS, aka loudness & ZCR, aka noisiness)
    rms = np.mean(librosa.feature.rms(y=y))
    zcr = np.mean(librosa.feature.zero_crossing_rate(y))

    # PROCESS: computing spectral shape features (centroid, bandwidth, rolloff, contrast)
    centroid = np.mean(librosa.feature.spectral_centroid(y=y, sr=sr))
    bandwidth = np.mean(librosa.feature.spectral_bandwidth(y=y, sr=sr))
    rolloff = np.mean(librosa.feature.spectral_rolloff(y=y, sr=sr))

    contrast = librosa.feature.spectral_contrast(y=y, sr=sr)
    contrast_mean = np.mean(contrast, axis=1)  # 7-dim

    # PROCESS: stacking all features
    extra = np.hstack(
        [
            f0_mean,  # main pitch estimate
            f0_conf,  # reliability of pitch
            rms,  # energy
            zcr,  # noisiness
            centroid,  # brightness
            bandwidth,  # width of spectrum
            rolloff,  # high-frequency content
            contrast_mean,  # 7-dim harmonic/noise separation
        ]
    )

    # PROCESS: combining into one fixed-length vector
    feature_vector = np.hstack(
        [mfcc_vector, extra]
    )  # final vector size = 120 + 14 = 134

    # OUTPUT:
    return feature_vector.astype(np.float32)
