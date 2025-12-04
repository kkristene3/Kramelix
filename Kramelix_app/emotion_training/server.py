import csv
import json
import os
import sys
import tempfile

import numpy as np
import onnxruntime as ort

from fastapi import FastAPI, UploadFile, File
from fastapi.responses import JSONResponse

from tests.melspec_cnn.test_sample_data_melspec import run_inference

# CONSTANT DECLARATION: ensuring project root is importable
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))
sys.path.append(PROJECT_ROOT)

LABEL_MAP_PATH = os.path.join(PROJECT_ROOT, "Kramelix_app\emotion_training\models", "label_map_melspec.json")

# OUTPUT: loading ONNX model
print("[INFO] Loading ONNX model...")

model_path = os.path.join(PROJECT_ROOT, "Kramelix_app\emotion_training\models", "audio_emotion_melspec.onnx")
sess_options = ort.SessionOptions()
session = ort.InferenceSession(model_path, sess_options, providers=["CPUExecutionProvider"])
input_name = session.get_inputs()[0].name

# PROCESS: loading label map
print("[INFO] Loading label map...")

with open(LABEL_MAP_PATH, "r") as f:
    label_map = json.load(f)

id_to_label = {v: k for k, v in label_map.items()}

app = FastAPI()

@app.post("/predict")
async def predict(audio: UploadFile = File(...)):
    try:
        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
            tmp.write(await audio.read())
            wav_path = tmp.name

        label, prompt = run_inference(wav_path, session, input_name, id_to_label)
        return label
    except Exception as e:
        print("error", str(e))
        return "ERROR: " + str(e)

@app.get("/ping")
async def ping():
    return {"status": "available"}
