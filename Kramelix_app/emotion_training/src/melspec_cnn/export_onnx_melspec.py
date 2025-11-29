"""
This module converts a trained Keras model (.h5) into an ONNX model
suitable for deployment in Android via ONNX Runtime.

Author: Amy Huang
Since: 1.0
"""

import tensorflow as tf
import tf2onnx


def export_to_onnx(keras_model_path: str, output_path: str, sample_input=None):
    """
    @brief
        Loads a trained Keras .h5 model & converts it into ONNX format.

    @param
        keras_model_path: str
            Path to the trained Keras model file (e.g. models/audio_emotion_melspec.h5).

    @param
        output_path: str
            Desired output path for the ONNX model (e.g. models/audio_emotion_melspec.onnx).

    @throws
        ValueError
            If sample_input is not provided or has incorrect shape.
    """

    print("[INFO] Loading trained Keras model...")

    # VARIABLE DECLARATION:
    model = tf.keras.models.load_model(keras_model_path)  # retrieving Keras model

    # PROCESS: validating sample input
    if sample_input is None:  # missing sample input
        # OUTPUT: raising error
        raise ValueError(
            "You MUST pass a sample_input w/ the correct CNN shape "
            "e.g. np.zeros((1, 64, 300, 1), dtype=np.float32)"
        )

    if sample_input.ndim != 4:  # wrong input shape
        # OUTPUT: raising error
        raise ValueError(
            f"Expected 4D CNN input (batch, n_mels, max_frames, 1), "
            f"but got shape {sample_input.shape}."
        )

    print("[INFO] Converting to ONNX...")

    # PROCESS: generating an ONNX graph from the Keras model
    model_proto, _ = tf2onnx.convert.from_keras(
        model,
        input_signature=(tf.TensorSpec(sample_input.shape, tf.float32),),
        opset=13,  # stables for Android ONNX Runtime
        output_path=output_path,  # writing directly to file
    )  # converting

    # OUTPUT: success (hopefully)
    print("[INFO] Export complete:", output_path)
