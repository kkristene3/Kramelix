"""
This module converts raw dataset samples into numerical feature vectors for model training,
w/ parallel processing for speedups during MFCC extraction.

Author: Amy Huang
Since: 1.0
"""

from typing import Dict, List, Tuple

import numpy as np
from joblib import Parallel, delayed

from ..dataset_loader import MASTER_SET
from .features_mfcc import extract_mfcc


def _process_single_sample(sample, label_map):
    """
    @brief
        Helper function used in parallel processing.
        Extracts one feature vector from a sample dict:
        { "path": "...", "label": "<emotion>" }

    @return
        (feature_vector, label_id) or None if failed
    """

    # VARIABLE DECLARATION:
    path = sample["path"]  # extracting path
    emotion = sample["label"]  # extracting emotion label

    # PROCESS: assigning int ID for each unique emotion
    if emotion not in label_map:  # not in master set
        # OUTPUT:
        print(f"[WARNING] Unknown emotion '{emotion}' in sample {path}. Skipped.")
        return None

    # PROCESS: extracting audio features
    try:

        # PROCESS: handling IEMOCAP segments if present
        if "start" in sample and "end" in sample:  # found
            feature_vector = extract_mfcc(
                path,
                start=sample["start"],
                end=sample["end"],
            )  # extracting segment

        else:  # just extract full file
            feature_vector = extract_mfcc(
                path,
            )

    except Exception as e:  # error-handling
        # OUTPUT:
        print(f"[WARNING] Failed to process file: {path} | Error: {e}")
        return None

    # OUTPUT:
    return (feature_vector, label_map[emotion])


def build_feature_dataset(samples: List[Dict]) -> Tuple[np.ndarray, np.ndarray, Dict]:
    """
    @brief
        Converts parsed dataset sample entries into numerical feature matrices.
        Each sample contains: { "path": "...", "label": "<emotion>" }

    @details
        MFCC features are extracted from each WAV file.
        Emotion strings are encoded using a fixed canonical ordering: ["neutral", "calm", "happy", "sad", "angry", "fearful", "disgust", "surprised"]
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

    @throws
        RuntimeError
            Raised if no valid MFCC features were extracted from the dataset.
    """

    # VARIABLE DECLARATION:
    label_map = {
        emotion: idx for idx, emotion in enumerate(MASTER_SET)
    }  # mapping master set strings to numeric classes

    # OUTPUT: debugging info
    print(f"[INFO] Beginning parallel MFCC extraction for {len(samples)} samples...")

    # PROCESS: parallel extraction across all CPU cores
    results = Parallel(
        n_jobs=-1,  # use ALL logical CPU cores
        backend="loky",  # isolated subprocesses (bc the features part is super heavy)
        verbose=5,  # progress logging
    )(delayed(_process_single_sample)(sample, label_map) for sample in samples)

    # PROCESS: filtering out None entries (failed files)
    valid = [r for r in results if r is not None]

    if len(valid) == 0:  # error-handling
        # OUTPUT: raise error
        raise RuntimeError("[ERROR] No valid MFCC features were extracted.")

    # PROCESS: separating into X (features) & y (labels)
    X, y = zip(*valid)

    # OUTPUT: converting to np.arrays
    return (
        np.array(X, dtype=np.float32),
        np.array(y, dtype=np.int32),
        label_map,
    )
