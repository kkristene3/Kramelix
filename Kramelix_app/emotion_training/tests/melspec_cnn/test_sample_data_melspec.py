# run `python -m tests.melspec_cnn.test_sample_data_melspec` to execute this script

"""
This test module batch-tests the ONNX Mel-CNN audio emotion classifier using
all WAV files inside the `tests/test_data/` folder.

It loads the CNN ONNX model, converts each WAV to a Mel spectrogram,
runs inference, prints results, & saves a CSV summary.

Author: Amy Huang
Since: 1.0
"""

import csv
import json
import os
import sys

import numpy as np
import onnxruntime as ort

from src.melspec_cnn.features_melspec import extract_melspec

# CONSTANT DECLARATION: ensuring project root is importable
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))
sys.path.append(PROJECT_ROOT)

# CONSTANT DECLARATION: paths
TEST_FOLDER = os.path.join(os.path.dirname(os.path.dirname(__file__)), "test_data")
CSV_OUTPUT = os.path.join(os.path.dirname(__file__), "test_results_melspec.csv")
LABEL_MAP_PATH = os.path.join(PROJECT_ROOT, "models", "label_map_melspec.json")


def run_inference(wav_path: str, session, input_name, id_to_label):
    """
    @brief
        Runs ONNX inference for a single WAV file.

    @param
        wav_path: str
            Path to the WAV file.

    @param
        session: onnxruntime.InferenceSession
            Pre-loaded ONNX runtime session.

    @param
        input_name: str
            Name of the ONNX model input.

    @param
        id_to_label: dict
            Mapping from label IDs to label names.
    """

    # OUTPUT:
    print(f"\n[INFO] Processing: {wav_path}")

    # PROCESS: extracting full feature vector
    mel = extract_melspec(wav_path)  # (64, 300)
    mel = np.expand_dims(mel, axis=(0, -1))  # (1, 64, 300, 1)

    # PROCESS: ONNX inference
    scores = session.run(None, {input_name: mel})[0][0]

    prediction_index = int(np.argmax(scores))
    prediction_label = id_to_label[prediction_index]
    prediction_prob = float(scores[prediction_index])

    # OUTPUT:
    print(f"Predicted: {prediction_label}")
    print(f"Confidence: {prediction_prob:.3f}")
    print("Scores:", scores)

    return prediction_label, prediction_prob


def main():
    """
    @brief
        Main function to run batch inference on all WAV files in the test folder.
    """

    # OUTPUT: loading ONNX model
    print("[INFO] Loading ONNX model...")

    model_path = os.path.join(PROJECT_ROOT, "models", "audio_emotion_melspec.onnx")
    session = ort.InferenceSession(model_path)

    # PROCESS: loading label map
    print("[INFO] Loading label map...")

    with open(LABEL_MAP_PATH, "r") as f:
        label_map = json.load(f)

    id_to_label = {v: k for k, v in label_map.items()}

    # PROCESS: pre-fetching ONNX input name
    input_name = session.get_inputs()[0].name

    # PROCESS: iterating through WAV files
    if not os.path.exists(TEST_FOLDER):  # error-handling
        # OUTPUT:
        print(f"[ERROR] Test folder '{TEST_FOLDER}' does not exist.")
        sys.exit(1)

    wav_files = [
        os.path.join(TEST_FOLDER, f)
        for f in os.listdir(TEST_FOLDER)
        if f.lower().endswith(".wav")
    ]  # extracting audio files

    if len(wav_files) == 0:  # error-handling
        # OUTPUT:
        print(f"[WARN] No WAV files found in '{TEST_FOLDER}'.")
        sys.exit(0)

    # OUTPUT:
    print(f"[INFO] Found {len(wav_files)} test files.\n")

    # PROCESS: opening CSV
    with open(CSV_OUTPUT, "w", newline="", encoding="utf-8") as csvfile:

        # VARIABLE DECLARATION: creating CSV writer
        writer = csv.writer(csvfile)
        writer.writerow(
            ["filename", "predicted_emotion", "confidence"]
        )  # writing header

        # PROCESS: iterating through WAV files
        for wav_path in wav_files:

            pred_label, pred_prob = run_inference(
                wav_path, session, input_name, id_to_label
            )  # running inference

            # PROCESS: writing to CSV
            writer.writerow(
                [
                    os.path.basename(wav_path),
                    pred_label,
                    f"{pred_prob:.3f}",
                ]
            )

    # OUTPUT:
    print(f"\n[INFO] CSV summary saved to: {CSV_OUTPUT}")


if __name__ == "__main__":
    main()
