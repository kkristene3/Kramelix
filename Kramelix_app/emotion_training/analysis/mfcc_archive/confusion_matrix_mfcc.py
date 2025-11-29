# run `python -m analysis.mfcc_archive.confusion_matrix_mfcc` to execute this script

"""
This module loads the precomputed feature matrix (X, y),
runs predictions using the trained model,
& plots the full confusion matrix.

Author: Amy Huang
Since: 1.0
"""

import os

import joblib
import matplotlib.pyplot as plt
import numpy as np
from sklearn.metrics import ConfusionMatrixDisplay, confusion_matrix
from tensorflow import keras

from src.dataset_loader import MASTER_SET

# CONSTANTS DECLARATION: path
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))


def main():

    # INITIALIZATION: loading saved features, scaler, & model
    print("[INFO] Loading saved features...")
    X = np.load(os.path.join(PROJECT_ROOT, "models", "features_X_mfcc.npy"))
    y = np.load(os.path.join(PROJECT_ROOT, "models", "features_y_mfcc.npy"))

    print("[INFO] Loading scaler...")
    scaler = joblib.load(os.path.join(PROJECT_ROOT, "models", "scaler_mfcc.pkl"))

    print("[INFO] Loading trained model...")
    model = keras.models.load_model(
        os.path.join(PROJECT_ROOT, "models", "audio_emotion_mfcc.h5")
    )

    # PROCESS: scaling features & running predictions
    print("[INFO] Scaling features...")
    X_scaled = scaler.transform(X)

    print("[INFO] Running batch predictions...")
    preds = model.predict(X_scaled, verbose=0)
    y_pred = np.argmax(preds, axis=1)

    # OUTPUT: building & plotting confusion matrix
    print("[INFO] Building confusion matrix...")
    cm = confusion_matrix(y, y_pred)

    display = ConfusionMatrixDisplay(confusion_matrix=cm, display_labels=MASTER_SET)

    plt.figure(figsize=(10, 8))
    display.plot(xticks_rotation=45, cmap="Blues", values_format="d")
    plt.title("Emotion Recognition - Confusion Matrix")
    plt.tight_layout()
    plt.show()


if __name__ == "__main__":
    main()
