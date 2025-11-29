"""
This module contains the main Keras-to-ONNX export pipeline entry point.

Author: Amy Huang
Since: 1.0
"""

import os

from src.mfcc_archive.export_onnx_mfcc import export_to_onnx

if __name__ == "__main__":

    # VARIABLE DECLARATION: setting the model paths
    keras_path = "models/audio_emotion_mfcc.h5"
    onnx_path = "models/audio_emotion_mfcc.onnx"

    if not os.path.exists(keras_path):  # Keras model missing
        print("[ERROR] Keras model not found:", keras_path)
        exit(1)  # aborting

    # PROCESS: calling helper function to export model
    export_to_onnx(keras_model_path=keras_path, output_path=onnx_path)
