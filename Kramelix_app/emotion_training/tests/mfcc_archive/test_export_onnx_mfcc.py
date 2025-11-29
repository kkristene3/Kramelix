# run `python -m tests.mfcc_archive.test_export_onnx_mfcc`

"""
This test module checks that the .h5 model converts nicely to an ONNX model & outputs the probabilities of the predicted emotions.

Author: Amy Huang
Since: 1.0
"""

import numpy as np
import onnxruntime as ort


def main():

    # OUTPUT: loading ONNX model
    print("[INFO] Loading ONNX model...")
    session = ort.InferenceSession("models/audio_emotion_mfcc.onnx")

    # VARIABLE DECLARATION: retrieving model's expected input shape
    input_meta = session.get_inputs()[0]
    input_name = input_meta.name
    input_shape = input_meta.shape

    feature_dim = input_shape[1]

    # OUTPUT:
    print(f"[INFO] Model expects input feature dim = {feature_dim}")

    # VARIABLE DECLARATION: creating dummy input w/ correct dimension
    dummy = np.random.rand(1, feature_dim).astype(np.float32)

    print("[INFO] Running inference...")
    output = session.run(None, {input_name: dummy})

    print("[INFO] Output probabilities:")
    print(output)


if __name__ == "__main__":
    main()
