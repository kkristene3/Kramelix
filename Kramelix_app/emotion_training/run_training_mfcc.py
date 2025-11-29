"""
This module contains the main training pipeline entry point.

Author: Amy Huang
Since: 1.0
"""

import json
import os

import joblib
import numpy as np

from src.dataset_loader import load_all_datasets
from src.mfcc_archive.preprocess_mfcc import build_feature_dataset
from src.mfcc_archive.train_mfcc import train_model

if __name__ == "__main__":

    # OUTPUT: the training pipeline
    print("[INFO] Loading dataset...")
    samples = load_all_datasets()

    print("[INFO] Extracting MFCC feature dataset...")
    X, y, label_map = build_feature_dataset(samples)

    print(f"[INFO] Feature matrix shape: {X.shape}")
    print(f"[INFO] Label map: {label_map}")

    print("[INFO] Training model...")
    model, history, scaler = train_model(X, y)

    # PROCESS: saving extracted feature matrix (for data analysis convenience later)
    np.save("models/features_X_mfcc.npy", X)
    np.save("models/features_y_mfcc.npy", y)
    print(
        "[INFO] Saved precomputed feature matrix: features_X_mfcc.npy & features_y_mfcc.npy"
    )

    # PROCESS: saving Keras .h5 model
    os.makedirs("models", exist_ok=True)
    model.save("models/audio_emotion_mfcc.h5")
    print("[INFO] Saved trained model to `models/audio_emotion_mfcc.h5`")

    # PROCESS: saving scaler for inference consistency
    joblib.dump(scaler, "models/scaler_mfcc.pkl")
    print(
        "[INFO] Saved feature scaler to `models/scaler_mfcc.pkl` with shape: {scaler.mean_.shape}"
    )

    # PROCESS: saving label map JSON
    with open("models/label_map_mfcc.json", "w") as f:
        json.dump(label_map, f)
    print("[INFO] Saved label map to `models/label_map_mfcc.json`")

    # PROCESS: saving training history JSON
    with open("models/history_mfcc.json", "w") as f:
        json.dump(history, f)
    print("[INFO] Saved training history to `models/history_mfcc.json`")

    print("[INFO] MFCC training complete.")
