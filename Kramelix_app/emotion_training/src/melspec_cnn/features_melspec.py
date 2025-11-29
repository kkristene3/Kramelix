"""
This module extracts robust audio features using librosa, including:
- Melspectrograms for CNN input

Author: Amy Huang
Since: 1.0
"""

import librosa
import numpy as np


def apply_spec_augment(mel, num_masks=2, freq_mask=8, time_mask=20):
    """
    @brief
        Applies SpecAugment to a Mel-spectrogram for data augmentation.

    @param
        mel: np.ndarray
            2D array (n_mels, max_frames) representing the log-Mel spectrogram.

        num_masks: int
            # of frequency and time masks to apply.

        freq_mask: int
            Width of each frequency mask (in Mel bins).

        time_mask: int
            Width of each time mask (in frames).

    @return
        np.ndarray
            Augmented Mel-spectrogram w/ SpecAugment applied.
    """
    # PROCESS: copying input to avoid modifying original
    mel = mel.copy()

    # VARIABLE DECLARATION: clamping mask widths to avoid overflows
    fm = min(freq_mask, mel.shape[0] - 1)
    tm = min(time_mask, mel.shape[1] - 1)

    # PROCESS: applying frequency masks
    for _ in range(num_masks):
        max_f = max(1, mel.shape[0] - fm)  # ensuring non-neg.

        f0 = np.random.randint(0, max_f)  # starting bin
        mel[f0 : f0 + fm, :] = 0.0  # masking by setting to 0

    # PROCESS: applying time masks
    for _ in range(num_masks):
        max_t = max(1, mel.shape[1] - tm)  # ensuring non-neg.

        t0 = np.random.randint(0, max_t)  # starting frame
        mel[:, t0 : t0 + tm] = 0.0  # masking by setting to 0

    # OUTPUT:
    return mel


def extract_melspec(
    path: str,
    n_mels: int = 64,
    max_frames: int = 300,
    start: float = None,
    end: float = None,
    augment=False,
) -> np.ndarray:
    """
    @brief
        Loads a WAV file & computes a log-Mel spectrogram suitable for CNN input.

    @details
        The Mel-spectrogram pipeline includes:
        1. Loading audio at a fixed sampling rate (16 kHz)
        2. Normalizing waveform amplitude
        3. Applying pre-emphasis to boost high-frequency cues
        4. Trimming leading/trailing silence
        5. Computing Mel power spectrogram
        6. Converting to log scale (dB)
        7. Normalizing per-utterance (zero-mean / unit-variance)
        8. Padding/cropping in time to a fixed # of frames

        Output shape: (n_mels, max_frames)

    @param
        path: str
            Absolute or relative path to the WAV file.

        n_mels: int
            Number of Mel frequency bins.

        max_frames: int
            Fixed # of time frames for CNN input (time axis is padded/cropped).

        start: float | None
            (Optional) Start time in seconds for segment extraction (IEMOCAP).

        end: float | None
            (Optional) End time in seconds for segment extraction (IEMOCAP).

        augment: bool
            Whether to apply SpecAugment for data augmentation (training only).

    @return
        np.ndarray (float32)
            2D array of shape (n_mels, max_frames) representing the log-Mel spectrogram.

    @throws
        Exception
            Throws exceptions related to file loading, invalid audio structure, or numerical failures.
            These are caught by preprocess_melspec.py later on.

        ValueError
            Raised if the audio file is empty after loading.
    """

    # VARIABLE DECLARATION: loading waveform at 16 kHz
    y, sr = librosa.load(path, sr=16000)  # y = waveform, sr = sampling rate

    # PROCESS: skipping empty audio
    if y.size == 0:
        raise ValueError(f"Empty audio file: {path}")

    # PROCESS: handling IEMOCAP segment paths
    if start is not None and end is not None:

        # VARIABLE DECLARATION: converting times to sample indices
        start_index = int(start * sr)
        end_index = int(end * sr)

        y = y[start_index:end_index]  # segmenting waveform

        if y.size == 0:  # empty segments
            # OUTPUT: raising error
            raise ValueError(
                f"IEMOCAP segment produced empty audio: {path} {start}-{end}"
            )

    # PROCESS: normalizing amplitude to [-1, 1]
    if np.max(np.abs(y)) > 0:
        y = y / np.max(np.abs(y))

    # PROCESS: boosting high frequencies (pre-emphasis)
    y = np.append(y[0], y[1:] - 0.97 * y[:-1])

    # PROCESS: trimming leading/trailing silence
    if len(y) > int(0.2 * sr):
        y, _ = librosa.effects.trim(y, top_db=40)

    # VARIABLE DECLARATION: windowing params
    n_fft = int(sr * 0.025)  # 25 ms window
    hop_length = int(sr * 0.010)  # 10 ms stride

    # PROCESS: computing Mel power spectrogram: (n_mels, time_frames)
    mel_spec = librosa.feature.melspectrogram(
        y=y,
        sr=sr,
        n_fft=n_fft,
        hop_length=hop_length,
        n_mels=n_mels,
        power=2.0,
    )

    # PROCESS: converting to log scale (dB) for perceptual loudness
    mel_db = librosa.power_to_db(mel_spec, ref=np.max)

    # PROCESS: per-utterance normalization (for CNN training stability)
    mean = np.mean(mel_db)
    std = np.std(mel_db)
    if std > 0:
        mel_db = (mel_db - mean) / std
    else:
        mel_db = mel_db - mean  # all-constant edge case

    # PROCESS: fixing time dimension -> padding/cropping to max_frames
    num_frames = mel_db.shape[1]

    if (
        num_frames < max_frames
    ):  # padding on the right w/ the minimum value (quiet background)
        pad_width = max_frames - num_frames
        pad = np.full((n_mels, pad_width), mel_db.min(), dtype=mel_db.dtype)
        mel_fixed = np.concatenate([mel_db, pad], axis=1)

    elif num_frames > max_frames:  # center-crop in time (keeps middle portion)
        start = (num_frames - max_frames) // 2
        mel_fixed = mel_db[:, start : start + max_frames]

    else:
        mel_fixed = mel_db

    # PROCESS: applying SpecAugment (only during training)
    if augment:
        mel_fixed = apply_spec_augment(mel_fixed)

    # OUTPUT:
    return mel_fixed.astype(np.float32)
