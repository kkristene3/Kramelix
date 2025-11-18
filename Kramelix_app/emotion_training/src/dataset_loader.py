"""
This module loads all the training datasets from the `data/` directory.

Author: Amy Huang
Since: 1.0
"""

import os

# CONSTANT DECLARATION: emotion mapping according to RAVDESS documentation
RAVDESS_EMOTION_MAP = {
    "01": "neutral",
    "02": "calm",
    "03": "happy",
    "04": "sad",
    "05": "angry",
    "06": "fearful",
    "07": "disgust",
    "08": "surprised",
}


def load_ravdess(path):
    """
    @brief
        Loads & parses audio files from the RAVDESS dataset directory.

    @param
        path: str
            Path to the RAVDESS root folder containing Actor_xx folders.

    @return
        List[dict]
            A list of sample dictionaries of the form:
            {
                "path": "<absolute/path/to/file.wav>",
                "label": "<emotion>"
            }
    """
    # DATASET LINK: https://www.kaggle.com/datasets/uwrfkaggler/ravdess-emotional-speech-audio

    # VARIABLE DECLARATION: list to accumulate parsed audio samples
    samples = []

    # PROCESS: recursively walking through RAVDESS directory structure
    for root, dirs, files in os.walk(path):

        # PROCESS: iterating through each file within Actor_xx directories
        for file in files:

            if file.endswith(".wav"):  # found WAV audio file

                # VARIABLE DECLARATION: parsing metadata from filename (refer to RAVDESS doc for info)
                parts = file.split(".")[0].split("-")

                emotion_code = parts[2]
                emotion_label = RAVDESS_EMOTION_MAP.get(emotion_code)

                if emotion_label is None:  # skipping unknown emotion codes
                    continue

                # PROCESS: appending parsed sample entry
                samples.append(
                    {
                        "path": os.path.join(root, file),
                        "label": emotion_label,
                    }
                )

    # OUTPUT:
    return samples


def load_all_datasets(root="data"):
    """
    @brief
        Aggregates all supported datasets into a unified sample list.
        It currently supports RAVDESS only, but is structured for future extension
        (e.g. CREMA-D, EmoDB, SAVEE).

    @param
        root: str
            Path to the data directory containing dataset subfolders.

    @return
        List[dict]
            A list of parsed samples across all supported datasets.
            {
                "path": "<absolute/path/to/file.wav>",
                "label": "<emotion>"
            }
    """

    # VARIABLE DECLARATION:
    samples = []  # master list for all dataset samples
    ravdess_path = os.path.join(root, "ravdess")  # expected RAVDESS directory

    # PROCESS: checking for RAVDESS dataset & loading if present
    if os.path.exists(ravdess_path):

        print("[INFO] Loading RAVDESS dataset...")  # debugging
        samples.extend(load_ravdess(ravdess_path))  # adding to list

    else:  # error-handling
        # OUTPUT:
        print("[WARNING] No RAVDESS folder found at:", ravdess_path)

    # OUTPUT: aggregated dataset samples
    return samples
