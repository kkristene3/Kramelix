"""
This module converts a trained Keras model (.h5) into an ONNX model
suitable for deployment in Android via ONNX Runtime.

Author: Amy Huang
Since: 1.0
"""

import tensorflow as tf
import tf2onnx


def export_to_onnx(keras_model_path: str, output_path: str):
    """
    @brief
        Loads a trained Keras .h5 model & converts it into ONNX format.

    @param
        keras_model_path: str
        Path to the trained Keras model file (e.g. models/audio_emotion.h5).

    @param
        output_path: str
        Desired output path for the ONNX model (e.g. models/audio_emotion.onnx).
    """

    print("[INFO] Loading trained Keras model...")

    # VARIABLE DECLARATION:
    model = tf.keras.models.load_model(keras_model_path)  # retrieving Keras model
    input_dim = model.input_shape[1]  # input shape (smth like (None, num_features))

    print("[INFO] Converting to ONNX...")

    # PROCESS: generating an ONNX graph from the Keras model
    spec = (tf.TensorSpec((None, input_dim), tf.float32, name="input"),)

    model_proto, _ = tf2onnx.convert.from_keras(
        model, input_signature=spec, output_path=output_path
    )  # converting

    # OUTPUT: success (hopefully)
    print(f"[INFO] Export complete to {output_path}")
