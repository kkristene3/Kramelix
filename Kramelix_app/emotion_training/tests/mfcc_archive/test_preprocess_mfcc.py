# run `python -m tests.mfcc_archive.test_preprocess_mfcc` to execute this file

"""
This test module dumps out the preprocess data.

Author: Amy Huang
Since: 1.0
"""

import numpy as np

from src.dataset_loader import load_all_datasets
from src.mfcc_archive.preprocess_mfcc import build_feature_dataset

# VARIABLE DECLARATION: loading the raw dataset info
samples = load_all_datasets()

# PROCESS: extracting MFCC & labels
X, y, label_map = build_feature_dataset(samples)

# OUTPUT:
print(
    "\n--------------------------------------------------------------------------------"
)
print("[INFO] Dataset Summary")
print(f"X shape: {X.shape}")  # (num_samples, 40) expected
print(f"y shape: {y.shape}")  # (num_samples,)
print(f"Labels: {label_map}")  # e.g. {"happy": 0, "sad": 1, ...}
print(f"Unique Emotion Count: {len(label_map)}")

# OUTPUT: showing the label distribution
unique, counts = np.unique(y, return_counts=True)  # counting each emotion

print(
    "\n--------------------------------------------------------------------------------"
)
print("[INFO] Label Distribution:")

for u, count in zip(unique, counts):
    emotion = [k for k, v in label_map.items() if v == u][0]  # extracting
    print(f"  {emotion:10s}: {count}")
