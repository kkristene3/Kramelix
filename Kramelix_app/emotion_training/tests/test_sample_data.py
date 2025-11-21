# run `python -m tests.test_sample_data`

"""
This test module batch-tests the ONNX audio emotion classifier using all WAV files inside the `tests/test_data/` folder.
It loads the model, extracts audio features (MFCC + Δ + ΔΔ + extras), applies the saved StandardScaler, & prints emotion predictions.
The full results are saved to `tests/test_results.csv`.

Author: Amy Huang
Since: 1.0
"""

import csv
import os
import sys

import joblib
import numpy as np
import onnxruntime as ort

from src.dataset_loader import MASTER_SET
from src.features import extract_mfcc

# CONSTANT DECLARATION: ensuring project root is importable
PROJECT_ROOT = os.path.dirname(os.path.dirname(__file__))
sys.path.append(PROJECT_ROOT)

# CONSTANT DECLARATION: paths
TEST_FOLDER = os.path.join(os.path.dirname(__file__), "test_data")
CSV_OUTPUT = os.path.join(os.path.dirname(__file__), "test_results.csv")


def run_inference(wav_path: str, session, scaler):
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
        scaler: StandardScaler
            Pre-fitted StandardScaler object.

    @return
        Tuple of (predicted_label: str, predicted_probability: float)
    """

    # OUTPUT:
    print(f"\n[INFO] Processing: {wav_path}")

    # PROCESS: extracting full feature vector
    features = extract_mfcc(wav_path)

    # PROCESS: reshaping & scaling
    features = features.reshape(1, -1).astype(np.float32)
    features = scaler.transform(features).astype(np.float32)

    # PROCESS: ONNX inference
    input_name = session.get_inputs()[0].name
    scores = session.run(None, {input_name: features})[0][0]

    prediction_index = int(np.argmax(scores))
    prediction_label = MASTER_SET[prediction_index]
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
    session = ort.InferenceSession(
        os.path.join(PROJECT_ROOT, "models", "audio_emotion.onnx")
    )

    # OUTPUT: loading scaler
    print("[INFO] Loading scaler...")
    scaler = joblib.load(os.path.join(PROJECT_ROOT, "models", "scaler.pkl"))

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
                wav_path, session, scaler
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
