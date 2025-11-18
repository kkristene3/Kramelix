"""
This module converts raw dataset samples into numerical MFCC tensors for modeltraining.
All emotion labels are normalized to a fixed canonical vocabulary to ensure cross-dataset consistency & stable label ordering.

Author: Amy Huang
Since: 1.0
"""

from typing import Dict, List, Tuple

import numpy as np

from .dataset_loader import MASTER_SET
from .features import extract_mfcc


def build_feature_dataset(samples: List[Dict]) -> Tuple[np.ndarray, np.ndarray, Dict]:
    """
    @brief
        Converts parsed dataset sample entries into numerical feature matrices.
        Each sample contains: { "path": "...", "label": "<emotion>" }

    @details
        MFCC features are extracted from each WAV file.
        Emotion strings are encoded using a fixed canonical ordering:
        ["neutral", "calm", "happy", "sad", "angry", "fearful", "disgust", "surprised"]
        This ensures consistent label IDs across training & inference.

    @param
        samples: List[dict]
            Raw dataset entries extracted from dataset_loader.

    @return
        (X, y, label_map)
            X: np.ndarray, shape (num_samples, num_features)
                MFCC feature vectors for each audio file.

            y: np.ndarray, shape (num_samples,)
                Integer emotion IDs consistent with `label_map`.

            label_map: dict
                Mapping of emotion string to integer ID.
    """

    # VARIABLE DECLARATION:
    X = []  # list of MFCC feature vectors
    y = []  # list of int emotion labels

    label_map = {
        emotion: index for index, emotion in enumerate(MASTER_SET)
    }  # mapping master set strings to numeric classes

    # PROCESS: handling every audio sample
    for sample in samples:

        path = sample["path"]  # extracting path
        emotion = sample["label"]  # extracting label

        # PROCESS: assigning int ID for each unique emotion
        if emotion not in label_map:  # not in master set
            print(f"[WARNING] Unknown emotion '{emotion}' in sample {path}. Skipped.")
            continue  # skipping

        # PROCESS: extracting MFCC feature vector from audio file
        try:
            feature_vector = extract_mfcc(path)
        except Exception as e:  # error-handling
            # OUTPUT:
            print(f"[WARNING] Failed to process file: {path} | Error: {e}")
            continue  # skipping

        # PROCESS: appending features & integer label
        X.append(feature_vector)
        y.append(label_map[emotion])

    # OUTPUT: the final feature dataset
    return (
        np.array(X, dtype=np.float32),
        np.array(y, dtype=np.int32),
        label_map,
    )
