"""
This module defines the training pipeline for the audio emotion recognition model
using log-Mel spectrograms and a CRNN (CNN + BiGRU).
It accepts preprocessed 4D tensors (num_samples, n_mels, max_frames, 1)
and trains a convolutional recurrent neural classifier.

Author: Amy Huang
Since: 1.0
"""

from typing import Dict, Tuple

import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.utils.class_weight import compute_class_weight
from tensorflow import keras
from tensorflow.keras import layers


# ---------------------------------------- MODEL ARCHITECTURE ----------------------------------------
def build_cnn_model(input_shape, num_classes: int) -> keras.Model:
    """
    @brief
        Builds a CRNN (CNN + BiGRU) for emotion recognition on log-Mel spectrograms.

    @param
        input_shape: tuple
            Shape of one input example, e.g. (n_mels, max_frames, 1).

        num_classes: int
            Total # of distinct emotion labels.

    @return
        Compiled Keras model ready for training.
    """

    # VARIABLE DECLARATION: regularization
    L2 = keras.regularizers.l2(1e-4)  # L2 to help reduce overfitting

    # VARIABLE DECLARATION: CRNN architecture
    model = keras.Sequential(
        [
            layers.Input(shape=input_shape),
            # CNN block 1
            layers.Conv2D(32, (3, 3), padding="same", kernel_regularizer=L2),
            layers.BatchNormalization(),
            layers.Activation("relu"),
            layers.MaxPooling2D((2, 2)),
            layers.Dropout(0.10),
            # Block 2
            layers.Conv2D(64, (3, 3), padding="same", kernel_regularizer=L2),
            layers.BatchNormalization(),
            layers.Activation("relu"),
            layers.MaxPooling2D((2, 2)),
            layers.Dropout(0.10),
            # Block 3
            layers.Conv2D(128, (3, 3), padding="same", kernel_regularizer=L2),
            layers.BatchNormalization(),
            layers.Activation("relu"),
            layers.MaxPooling2D((2, 2)),
            layers.Dropout(0.15),
            # Reshape for RNN
            layers.Reshape((-1, 128)),
            # GRU head
            layers.Bidirectional(
                layers.GRU(
                    96,
                    return_sequences=False,
                    dropout=0.2,
                )
            ),
            # dense head
            layers.Dense(256, activation="relu"),
            layers.Dropout(0.30),
            layers.Dense(128, activation="relu"),
            layers.Dropout(0.30),
            layers.Dense(num_classes, activation="softmax"),
        ]
    )

    # PROCESS: compiling model w/ optimizer, loss, & metrics
    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=0.0007),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )

    # OUTPUT:
    return model


# ---------------------------------------- TRAINING PIPELINE ----------------------------------------
def train_cnn(
    X: np.ndarray,
    y: np.ndarray,
    epochs: int = 50,
    batch_size: int = 32,
) -> Tuple[keras.Model, Dict, Tuple[np.ndarray, np.ndarray]]:
    """
    @brief
        Trains the CRNN classifier using log-Mel spectrogram tensors,
        w/ class weighting & stratified train/validation split.

    @param
        X: np.ndarray
            Feature tensor, shape (num_samples, n_mels, max_frames, 1).

    @param
        y: np.ndarray
            Int labels, shape (num_samples,).

    @param
        epochs: int
            # of training epochs.

    @param
        batch_size: int
            Mini-batch size.

    @return
        (model, history, test_split)
            model: Trained Keras model
            history: Training history dictionary (loss/accuracy curves)
            test_split: (X_test, y_test) tuple for final evaluation
    """

    # PROCESS: class weights for imbalanced emotions
    classes = np.unique(y)
    class_weights_arr = compute_class_weight(
        class_weight="balanced", classes=classes, y=y
    )
    class_weights = {int(c): float(w) for c, w in zip(classes, class_weights_arr)}

    # OUTPUT: debugging info
    print("\n[INFO] Computed class weights (CNN):", class_weights)

    # PROCESS: train/validation/test split (70/15/15)
    X_train, X_temp, y_train, y_temp = train_test_split(
        X,
        y,
        test_size=0.30,
        shuffle=True,
        stratify=y,
        random_state=42,
    )

    X_val, X_test, y_val, y_test = train_test_split(
        X_temp,
        y_temp,
        test_size=0.50,
        shuffle=True,
        stratify=y_temp,
        random_state=42,
    )

    # OUTPUT: debugging info
    print("[INFO] Dataset split:")
    print(f"  Train: {len(X_train)} samples")
    print(f"  Val:   {len(X_val)} samples")
    print(f"  Test:  {len(X_test)} samples")

    # INITIALIZATION: building CRNN model
    input_shape = X.shape[1:]
    model = build_cnn_model(input_shape=input_shape, num_classes=len(classes))

    # PROCESS: adding early stopping to avoid overfitting
    callbacks = [
        keras.callbacks.EarlyStopping(
            patience=6, monitor="val_loss", restore_best_weights=True
        )
    ]

    # PROCESS: training loop
    history = model.fit(
        X_train,
        y_train,
        validation_data=(X_val, y_val),
        epochs=epochs,
        batch_size=batch_size,
        class_weight=class_weights,
        callbacks=callbacks,
        verbose=1,
    )

    # OUTPUT:
    return model, history.history, (X_test, y_test)
