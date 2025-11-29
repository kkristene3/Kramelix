# run: python -m analysis.melspec_cnn.confusion_matrix_melspec

"""
This module loads the precomputed feature matrix (X, y),
runs predictions using the trained Mel-CNN model,
& plots the confusion matrix.

Author: Amy Huang
Since: 1.0
"""

import json
import os

import matplotlib.pyplot as plt
import numpy as np
from sklearn.metrics import ConfusionMatrixDisplay, confusion_matrix
from tensorflow.keras.models import load_model

# CONSTANTS: paths
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))
MODEL_PATH = os.path.join(PROJECT_ROOT, "models", "audio_emotion_melspec.h5")
X_PATH = os.path.join(PROJECT_ROOT, "models", "features_X_melspec.npy")
Y_PATH = os.path.join(PROJECT_ROOT, "models", "features_y_melspec.npy")
LABEL_MAP_PATH = os.path.join(PROJECT_ROOT, "models", "label_map_melspec.json")


def main():

    if not os.path.exists(X_PATH) or not os.path.exists(Y_PATH):  # error-handling
        # OUTPUT:
        print("[ERROR] Mel feature files not found:")
        print(f"  {X_PATH}")
        print(f"  {Y_PATH}")
        return

    # INITIALIZATION: loading saved features & label map
    print("[INFO] Loading saved Mel-spectrogram features...")
    X = np.load(X_PATH)
    y = np.load(Y_PATH)

    print(f"[INFO] Loaded X: {X.shape}, y: {y.shape}")

    print("[INFO] Loading label map...")
    with open(LABEL_MAP_PATH, "r") as f:
        label_map = json.load(f)

    # PROCESS: converting {label -> id} into sorted list of labels in correct index order
    id_to_label = {v: k for k, v in label_map.items()}
    labels = [id_to_label[i] for i in range(len(id_to_label))]

    print("[INFO] Loading Mel-CNN model...")
    model = load_model(MODEL_PATH, compile=False)

    # PROCESS: running predictions
    print("[INFO] Running predictions...")
    y_pred_prob = model.predict(X, verbose=0)
    y_pred = np.argmax(y_pred_prob, axis=1)

    # OUTPUT: building & plotting confusion matrix
    print("[INFO] Building confusion matrix...")
    conmat = confusion_matrix(y, y_pred)

    display = ConfusionMatrixDisplay(conmat, display_labels=labels)

    plt.figure(figsize=(10, 8))
    display.plot(xticks_rotation=45, cmap="Blues", values_format="d")
    plt.title("Mel-CNN - Confusion Matrix")
    plt.tight_layout()
    plt.show()


if __name__ == "__main__":
    main()
