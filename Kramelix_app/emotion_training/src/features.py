"""
This module extracts robust audio features (MFCC + Δ + ΔΔ) using librosa.

Author: Amy Huang
Since: 1.0
"""

import librosa
import numpy as np


def extract_mfcc(path: str, mfcc_count: int = 40) -> np.ndarray:
    """
    @brief
        Loads a WAV file & computes its MFCC feature representation consisting of:
        - MFCC coefficients
        - MFCC delta (1st-order time derivative)
        - MFCC delta-delta (2nd-order time derivative)

    @details
        The MFCC extraction pipeline includes:
        1. Loading audio at a fixed sampling rate (16 kHz)
        2. Optional trimming of leading/trailing silence
        3. Amplitude normalization
        4. Conversion to MFCC coefficients
        5. Concatenation of [MFCC | Δ | ΔΔ], followed by time-averaging

    @param
        path: str
            Absolute or relative path to the WAV file.

    @param
        mfcc_count: int
            # of MFCC coefficients (dimensions) to compute.

    @return
        np.ndarray (float32)
            Vector of shape (mfcc_count * 3,) representing [MFCC | Δ | ΔΔ], averaged across time.

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

    # PROCESS: normalizing the amplitude to safe numerical range (per-sample)
    if y.std() > 0:
        y = (y - np.mean(y)) / np.std(y)

    # VARIABLE DECLARATION: windowing parameters
    n_fft = int(sr * 0.025)  # 25 ms window
    hop_length = int(sr * 0.010)  # 10 ms hop

    # PROCESS: computing MFCC base matrix
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

    # PROCESS: stacking into a single matrix: (3 * mfcc_count, time_frames)
    combined = np.vstack([mfcc, delta, delta2])

    # PROCESS: averaging across time to obtain a fixed-length vector
    feature_vector = np.mean(combined, axis=1)

    # OUTPUT:
    return feature_vector.astype(np.float32)
