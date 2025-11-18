"""
This module defines the training pipeline for the audio emotion recognition model.
It accepts preprocessed MFCC tensors & trains a simple feed-forward neural classifier.

Author: Amy Huang
Since: 1.0
"""

from typing import Dict, Tuple

import numpy as np
from sklearn.model_selection import train_test_split
from tensorflow import keras
from tensorflow.keras import layers


def build_model(input_dim: int, num_classes: int) -> keras.Model:
    """
    @brief
        Builds a feed-forward neural classifier for emotion recognition,
        based on MFCC features.

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
            layers.Dense(256, activation="relu"),
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


def train_model(X: np.ndarray, y: np.ndarray) -> Tuple[keras.Model, Dict]:
    """
    @brief
        Trains the emotion classifier model using MFCC features.
        Automatically splits the dataset into training/validation sets.

    @param
        X: np.ndarray
        Feature matrix shape (num_samples, num_features).

    @param
        y: np.ndarray
        Int labels shape (num_samples,).

    @return
        (model, history)
            model: Trained Keras model
            history: Training history dictionary
    """

    # VARIABLE DECLARATION: training/validation split
    X_train, X_val, y_train, y_val = train_test_split(
        X, y, test_size=0.2, shuffle=True, stratify=y, random_state=42
    )

    # INITIALIZATION: building classifier
    model = build_model(input_dim=X.shape[1], num_classes=len(np.unique(y)))

    # PROCESS: performing training epoch loop
    history = model.fit(
        X_train,
        y_train,
        validation_data=(X_val, y_val),
        epochs=40,
        batch_size=32,
        verbose=1,
    )

    # OUTPUT:
    return model, history.history
