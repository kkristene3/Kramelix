"""
This module contains the main training pipeline entry point.

Author: Amy Huang
Since: 1.0
"""

import json
import os

import joblib

from src.dataset_loader import load_all_datasets
from src.preprocess import build_feature_dataset
from src.train import train_model

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

    # PROCESS: saving Keras .h5 model
    os.makedirs("models", exist_ok=True)
    model.save("models/audio_emotion.h5")

    print("[INFO] Saved trained model to `models/audio_emotion.h5`")

    # PROCESS: saving label map JSON
    with open("models/label_map.json", "w") as f:
        json.dump(label_map, f)
    print("[INFO] Saved label map to `models/label_map.json`")

    # PROCESS: saving scaler for inference consistency
    joblib.dump(scaler, "models/scaler.pkl")
    print(
        "[INFO] Saved feature scaler to `models/scaler.pkl` with shape: {scaler.mean_.shape}"
    )

    print("[INFO] Training complete.")
