"""
This module contains the main Keras-to-ONNX export pipeline entry point.

Author: Amy Huang
Since: 1.0
"""

import os

import numpy as np

from src.melspec_cnn.export_onnx_melspec import export_to_onnx

if __name__ == "__main__":

    # VARIABLE DECLARATION: setting the model paths
    keras_path = "models/audio_emotion_melspec.h5"
    onnx_path = "models/audio_emotion_melspec.onnx"

    if not os.path.exists(keras_path):  # Keras model missing
        print("[ERROR] Keras model not found:", keras_path)
        exit(1)  # aborting

    dummy_input = np.random.rand(1, 64, 300, 1).astype(
        np.float32
    )  # used for tracing CNN shape: (1, 64, 300, 1)

    # PROCESS: calling helper function to export model
    export_to_onnx(
        keras_model_path=keras_path, output_path=onnx_path, sample_input=dummy_input
    )
