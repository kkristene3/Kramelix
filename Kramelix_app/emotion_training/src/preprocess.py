"""
This module converts raw dataset samples into numerical feature tensors using
MFCC feature extraction. It produces (X, y, label_map) for model training.
"""

from typing import Dict, List

import numpy as np

from .features import extract_mfcc


def build_feature_dataset(samples: List[Dict]):
    """
    @brief
        This function converts a list of audio samples (path & emotion label) into numerical
        model-ready datasets. It extracts MFCC features for each audio file &
        encodes emotion labels into int classes.

    @param
        samples: List[dict]
            List of dataset entries of the form:
            {
                "path": "<absolute/audio/path.wav>",
                "label": "<emotion>"
            }

    @return
        (X, y, label_map)
            X: np.ndarray, shape (num_samples, num_features)
                MFCC feature vectors for each audio file.

            y: np.ndarray, shape (num_samples,)
                Integer-encoded emotion labels.

            label_map: dict
                Mapping of emotion string to integer ID.
                Ex: { "happy": 0, "sad": 1, ... }
    """

    # VARIABLE DECLARATION:
    X = []  # list of MFCC feature vectors
    y = []  # list of int emotion labels

    label_map = {}  # mapping for emotion string to numeric class
    label_counter = 0

    # PROCESS: handling every audio sample
    for sample in samples:

        path = sample["path"]  # extracting path
        emotion = sample["label"]  # ... label

        # PROCESS: assigning int ID for each unique emotion
        if emotion not in label_map:  # not yet assigned

            label_map[emotion] = label_counter
            label_counter += 1  # updating count

        # PROCESS: extracting MFCC feature vector from audio file
        try:
            feature_vector = extract_mfcc(path)
        except Exception as e:  # error-handling
            # OUTPUT:
            print(f"[WARNING] Failed to process file: {path} | Error: {e}")
            continue  # skipping

        # PROCESS: updating lists
        X.append(feature_vector)
        y.append(label_map[emotion])

    # OUTPUT: the final feature dataset
    return (
        np.array(X, dtype=np.float32),
        np.array(y, dtype=np.int32),
        label_map,
    )
