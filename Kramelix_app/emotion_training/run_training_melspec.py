"""
This module contains the main training pipeline entry point for the Mel-CNN model.

Author: Amy Huang
Since: 1.0
"""

import json
import os

import numpy as np

from src.dataset_loader import load_all_datasets
from src.melspec_cnn.preprocess_melspec import build_melspec_dataset
from src.melspec_cnn.train_melspec import train_cnn

if __name__ == "__main__":

    # OUTPUT: the training pipeline
    print("[INFO] Loading dataset...")
    samples = load_all_datasets()

    print("[INFO] Building Mel-spectrogram dataset...")
    X, y, label_map = build_melspec_dataset(samples)

    print(f"[INFO] Feature tensor shape: {X.shape}")
    print(f"[INFO] Label map: {label_map}")

    print("[INFO] Training Mel-CNN model...")
    model, history, test_split = train_cnn(X, y)

    # PROCESS: saving extracted feature matrix (for data analysis convenience later)
    np.save("models/features_X_melspec.npy", X)
    np.save("models/features_y_melspec.npy", y)
    print(
        "[INFO] Saved precomputed feature matrix: features_X_melspec.npy & features_y_melspec.npy"
    )

    # PROCESS: saving Keras .h5 model
    os.makedirs("models", exist_ok=True)
    model.save("models/audio_emotion_melspec.h5")
    print("[INFO] Saved Mel-CNN model to `models/audio_emotion_melspec.h5`")

    # PROCESS: saving label map JSON
    with open("models/label_map_melspec.json", "w") as f:
        json.dump(label_map, f)
    print("[INFO] Saved label map to `models/label_map_melspec.json`")

    # PROCESS: saving training history JSON
    with open("models/history_melspec.json", "w") as f:
        json.dump(history, f)
    print("[INFO] Saved training history to `models/history_melspec.json`")

    print("[INFO] Mel-CNN training complete.")
