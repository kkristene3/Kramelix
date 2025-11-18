"""
This module extracts audio features (MFCCs only for now) using librosa.

Author: Amy Huang
Since: 1.0
"""

import librosa
import numpy as np


def extract_mfcc(path: str, mfcc_count: int = 40) -> np.ndarray:
    """
    @brief
        This function loads an audio file (WAV) & computes its MFCC feature representation.

    @details
        The MFCC extraction pipeline includes:
            1. Loading audio at a fixed sampling rate (16 kHz)
            2. Optional trimming of leading/trailing silence
            3. Amplitude normalization
            4. Conversion to MFCC coefficients
            5. Time averaging to obtain a stable feature vector

    @param
        path: str
        Absolute or relative path to the WAV file.

    @param
        mfcc_count: int
        # of MFCC coefficients (dimensions) to compute.

    @return
        np.ndarray
            1D numpy vector of shape (n_mfcc,), representing the averaged MFCC
            features for the input audio clip.

    @throws Exception
        Throws exceptions related to file loading, invalid audio structure,
        or numerical failures. These are caught by preprocess.py later on.
    """

    # VARIABLE DECLARATION: loading waveform at 16 kHz
    # (AMY'S NOTE: I think this is a common ML standard?)
    y, sr = librosa.load(path, sr=16000)  # y = waveform, sr = sampling rate

    # PROCESS: normaizing the amplitude to safe numerical range
    if len(y) > 0:  # needs normalization
        y = librosa.util.normalize(y)

    # PROCESS: removing leading/trailing silence to stabilize MFCCs
    y, _ = librosa.effects.trim(y, top_db=40)

    # PROCESS: computing MFCC matrix shape (n_mfcc, time_frames)
    mfcc_matrix = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=mfcc_count)

    # PROCESS: avging across time to obtain fixed-length vector
    mfcc_vector = np.mean(mfcc_matrix.T, axis=0)

    # OUTPUT:
    return mfcc_vector.astype(np.float32)
