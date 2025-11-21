"""
This module defines the training pipeline for the audio emotion recognition model.
It accepts preprocessed MFCC tensors & trains a feed-forward neural classifier
with global feature normalization, class weighting, and dataset shuffling.

Author: Amy Huang
Since: 1.0
"""

from typing import Dict, Tuple

import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.utils.class_weight import compute_class_weight
from tensorflow import keras
from tensorflow.keras import layers


# ---------------------------------------- MODEL ARCHITECTURE ----------------------------------------
def build_model(input_dim: int, num_classes: int) -> keras.Model:
    """
    @brief
        Builds a feed-forward neural classifier for emotion recognition, based on MFCC features.

    @details
        Uses BatchNorm + Dropout to stabilize multi-dataset training.

    @param
        input_dim: int
            # of MFCC coefficients per sample (aka feature vector length).

    @param
        num_classes: int
            Total # of distinct emotion labels.

    @return
        Compiled Keras model ready for training.
    """

    # VARIABLE DECLARATION: model architecture
    model = keras.Sequential(
        [
            layers.Input(shape=(input_dim,)),
            layers.Dense(512, activation="relu"),
            layers.BatchNormalization(),
            layers.Dropout(0.3),
            layers.Dense(256, activation="relu"),
            layers.BatchNormalization(),
            layers.Dropout(0.3),
            layers.Dense(128, activation="relu"),
            layers.Dropout(0.2),
            layers.Dense(
                num_classes, activation="softmax"
            ),  # using softmax to limit emotion identification to ONE class
        ]
    )

    # PROCESS: compiling model w/ optimizer, loss, & metrics
    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=0.001),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )

    # OUTPUT:
    return model


# ---------------------------------------- TRAINING PIPELINE ----------------------------------------
def train_model(
    X: np.ndarray, y: np.ndarray
) -> Tuple[keras.Model, Dict, StandardScaler]:
    """
    @brief
        Trains the emotion classifier using MFCC features, w/ class balancing,
        dataset shuffling, feature normalization, and validation split.

    @param
        X: np.ndarray
            Feature matrix shape (num_samples, num_features).

    @param
        y: np.ndarray
            Int labels shape (num_samples,).

    @return
        (model, history, scaler)
            model: Trained Keras model
            history: Training history dictionary
            scaler: Feature normalizer used for inference
    """

    # PROCESS: random shuffle (ensuring datasets are mixed to prevent class collapse)
    rng = np.random.default_rng(seed=42)
    indices = rng.permutation(len(X))

    X = X[indices]
    y = y[indices]

    # VARIABLE DECLARATION: global feature scaler (req. for inference consistency)
    scaler = StandardScaler()

    # PROCESS: fitting on entire dataset before splitting
    X = scaler.fit_transform(X)

    # PROCESS: automatically rebalance skewed emotion counts w/ class weights
    classes = np.unique(y)

    class_weights = compute_class_weight(class_weight="balanced", classes=classes, y=y)
    class_weights = {i: w for i, w in enumerate(class_weights)}

    # OUTPUT: debugging dump
    print("\n[INFO] Computed class weights:", class_weights)

    # PROCESS: stratified split to maintain class distribution in train/val sets
    X_train, X_val, y_train, y_val = train_test_split(
        X, y, test_size=0.2, shuffle=True, stratify=y, random_state=42
    )

    # INITIALIZATION: building classifier
    model = build_model(input_dim=X.shape[1], num_classes=len(classes))

    # PROCESS: performing training epoch loop
    history = model.fit(
        X_train,
        y_train,
        validation_data=(X_val, y_val),
        epochs=60,
        batch_size=32,
        class_weight=class_weights,
        verbose=1,
    )

    # OUTPUT:
    return model, history.history, scaler
