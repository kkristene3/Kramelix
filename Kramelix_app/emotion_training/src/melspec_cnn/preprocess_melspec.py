"""
This module converts raw dataset samples into log-Mel spectrogram tensors for model training.

Author: Amy Huang
Since: 1.0
"""

from typing import Dict, List, Tuple

import numpy as np
from joblib import Parallel, delayed

from ..dataset_loader import MASTER_SET
from ..melspec_cnn.features_melspec import extract_melspec


def _process_single_sample(sample, label_map, n_mels, max_frames):
    """
    @brief
        Helper function used in parallel processing.
        Extracts one log-Mel spectrogram tensor from a sample dict:
        { "path": "...", "label": "<emotion>" }

    @return
        (mel_tensor, label_id) or None if failed
    """

    # VARIABLE DECLARATION:
    path = sample["path"]  # extracting path
    emotion = sample["label"]  # extracting emotion label

    # PROCESS: assigning int ID for each unique emotion
    if emotion not in label_map:  # not in master set
        # OUTPUT:
        print(f"[WARNING] Unknown emotion '{emotion}' in sample {path}. Skipped.")
        return None

    # VARIABLE DECLARATION: augmentation flag
    augment_flag = np.random.rand() < 0.5  # 50% of samples are augmented

    # PROCESS: extracting log-Mel spectrogram features
    try:

        # PROCESS: handling IEMOCAP segments if present
        if "start" in sample and "end" in sample:  # found
            mel = extract_melspec(
                path,
                start=sample["start"],
                end=sample["end"],
                n_mels=n_mels,
                max_frames=max_frames,
                augment=augment_flag,
            )  # extracting segment

        else:  # just extract full file

            mel = extract_melspec(
                path, n_mels=n_mels, max_frames=max_frames, augment=augment_flag
            )

    except Exception as e:  # error-handling
        # OUTPUT:
        print(f"[WARNING] Failed to process file: {path} | Error: {e}")
        return None

    # PROCESS: adding channel dimension for CNN: (n_mels, max_frames, 1)
    mel = np.expand_dims(mel, axis=-1)

    # OUTPUT:
    return mel, label_map[emotion]


def build_melspec_dataset(
    samples: List[Dict],
    n_mels: int = 64,
    max_frames: int = 300,
) -> Tuple[np.ndarray, np.ndarray, Dict]:
    """
    @brief
        Converts parsed dataset sample entries into log-Mel spectrogram tensors
        suitable for CNN input.

    @details
        Each WAV file is processed as follows:
        1. Loading the audio & extracting a (n_mels, max_frames) log-Mel spectrogram using extract_melspec()
        2. Converting emotion string to integer ID
        3. Aggregating into:
            - X: 4D tensor, shape (num_samples, n_mels, max_frames, 1)
            - y: 1D int labels

        Emotion strings follow the fixed canonical ordering from `MASTER_SET`:
        ["neutral", "calm", "happy", "sad", "angry", "fearful", "disgust", "surprised"]

    @throws
        RuntimeError
            Raised if no valid Mel-spectrograms were extracted from the dataset.
    """

    # VARIABLE DECLARATION:
    label_map = {
        emotion: idx for idx, emotion in enumerate(MASTER_SET)
    }  # mapping master set strings to numeric classes

    # OUTPUT: debugging info
    print(
        f"[INFO] Beginning parallel Mel-spectrogram extraction for {len(samples)} samples..."
    )

    # PROCESS: parallel extraction across all CPU cores
    results = Parallel(
        n_jobs=-1,  # use ALL logical CPU cores
        backend="loky",  # isolated processes (important for librosa stability)
        verbose=5,  # progress logging
    )(
        delayed(_process_single_sample)(sample, label_map, n_mels, max_frames)
        for sample in samples
    )

    # PROCESS: filtering out None entries (failed files)
    valid = [r for r in results if r is not None]

    if len(valid) == 0:  # error-handling
        # OUTPUT: raise error
        raise RuntimeError("[ERROR] No valid Mel-spectrograms were extracted.")

    # PROCESS: separating into X (tensors) & y (labels)
    X, y = zip(*valid)

    # OUTPUT: convert to arrays
    X_arr = np.stack(X, axis=0).astype(np.float32)
    y_arr = np.array(y, dtype=np.int32)

    return X_arr, y_arr, label_map
