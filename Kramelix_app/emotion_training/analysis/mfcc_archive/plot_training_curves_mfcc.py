# run `python -m analysis.mfcc_archive.plot_training_curves_mfcc` to execute this script

"""
This module plots the training/validation accuracy & loss curves from the saved history_mfcc.json file.

Author: Amy Huang
Since: 1.0
"""

import json
import os

import matplotlib.pyplot as plt

# CONSTANTS DECLARATION: paths
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))
HISTORY_PATH = os.path.join(PROJECT_ROOT, "models", "history_mfcc.json")


def main():

    if not os.path.exists(HISTORY_PATH):  # error-handling
        # OUTPUT:
        print(f"[ERROR] history_mfcc.json not found at: {HISTORY_PATH}")
        return

    # PROCESS: loading training history JSON
    with open(HISTORY_PATH, "r") as f:
        history = json.load(f)

    # VARIABLE DECLARATION: extracting metrics
    acc = history.get("accuracy", [])
    val_acc = history.get("val_accuracy", [])
    loss = history.get("loss", [])
    val_loss = history.get("val_loss", [])

    epochs = range(1, len(acc) + 1)

    # OUTPUT: plotting accuracy curve
    plt.figure(figsize=(10, 4))
    plt.plot(epochs, acc, label="Train Accuracy")
    plt.plot(epochs, val_acc, label="Val Accuracy")
    plt.xlabel("Epoch")
    plt.ylabel("Accuracy")
    plt.title("Training vs. Validation Accuracy")
    plt.legend()
    plt.grid(True)
    plt.tight_layout()
    plt.show()

    # OUTPUT: plotting loss curve
    plt.figure(figsize=(10, 4))
    plt.plot(epochs, loss, label="Train Loss")
    plt.plot(epochs, val_loss, label="Val Loss")
    plt.xlabel("Epoch")
    plt.ylabel("Loss")
    plt.title("Training vs. Validation Loss")
    plt.legend()
    plt.grid(True)
    plt.tight_layout()
    plt.show()


if __name__ == "__main__":
    main()
