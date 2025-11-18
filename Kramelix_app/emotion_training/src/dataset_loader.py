"""
This module loads all supported emotion-speech datasets from the `data/` directory
& normalizes their labels to a unified emotion vocabulary.

Datasets supported:
1. RAVDESS
2. CREMA-D
3. EmoDB
4. SAVEE

Author: Amy Huang
Since: 1.0
"""

import os

# CONSTANT DECLARATION: all dataset-specific emotions will be mapped to one in this master set
MASTER_SET = [
    "neutral",
    "calm",
    "happy",
    "sad",
    "angry",
    "fearful",
    "disgust",
    "surprised",
]


# ---------------------------------------- RAVDESS ----------------------------------------
# DATASET LINK: https://www.kaggle.com/datasets/uwrfkaggler/ravdess-emotional-speech-audio

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


# ---------------------------------------- CREMA-D ----------------------------------------
# DATASET LINK: https://www.kaggle.com/datasets/ejlok1/cremad

# CONSTANT DECLARATION: emotion mapping according to CREMA-D documentation
CREMAD_MAP = {
    "ANG": "angry",
    "DIS": "disgust",
    "FEA": "fearful",
    "HAP": "happy",
    "NEU": "neutral",
    "SAD": "sad",
}


def load_cremad(path):
    """
    @brief
        Loads & parses audio files from the CREMA-D dataset directory.

    @param
        path: str
            Path to CREMA-D directory with .wav files.

    @return
        List[dict]
            A list of sample dictionaries of the form:
            {
                "path": "<absolute/path/to/file.wav>",
                "label": "<emotion>"
            }
    """
    # VARIABLE DECLARATION: list to accumulate parsed audio samples
    samples = []

    # PROCESS: recursively walking through CREMA-D directory structure
    for file in os.listdir(path):

        if not file.endswith(".wav"):  # skipping non-WAV files
            continue

        # VARIABLE DECLARATION: parsing metadata from filename (refer to CREMA-D doc for info)
        parts = file.split("_")

        emotion_code = parts[2]  # e.g. ANG / SAD / FEA
        emotion_label = CREMAD_MAP.get(emotion_code)

        if emotion_label:
            # PROCESS: appending parsed sample entry
            samples.append(
                {
                    "path": os.path.join(path, file),
                    "label": emotion_label,
                }
            )

    # OUTPUT:
    return samples


# ---------------------------------------- EmoDB ----------------------------------------
# DATASET LINK: https://www.kaggle.com/datasets/ejlok1/toronto-emotional-speech-set-tess

# CONSTANT DECLARATION: emotion mapping according to EmoDB documentation
EMODB_MAP = {
    "W": "angry",
    "E": "disgust",
    "A": "fearful",
    "F": "happy",
    "T": "sad",
    "N": "neutral",
    "L": "calm",  # "boredom" is closest to calm
}


def load_emodb(path):
    """
    @brief
        Loads & parses audio files from the EmoDB dataset directory.

    @param
        path: str
            Path to the EmoDB root folder containing WAV files.

    @return
        List[dict]
            A list of sample dictionaries of the form:
            {
                "path": "<absolute/path/to/file.wav>",
                "label": "<emotion>"
            }
    """
    # VARIABLE DECLARATION: list to accumulate parsed audio samples
    samples = []

    # PROCESS: recursively walking through EmoDB directory structure
    for file in os.listdir(path):

        if not file.endswith(".wav"):  # skipping non-WAV files
            continue

        # VARIABLE DECLARATION: parsing metadata from filename (refer to EmoDB doc for info)
        emotion_code = file[5].upper()
        emotion_label = EMODB_MAP.get(emotion_code)

        if emotion_label:
            # PROCESS: appending parsed sample entry
            samples.append(
                {
                    "path": os.path.join(path, file),
                    "label": emotion_label,
                }
            )

    # OUTPUT:
    return samples


# ---------------------------------------- SAVEE ----------------------------------------
# DATASET LINK: https://www.kaggle.com/datasets/ejlok1/surrey-audiovisual-expressed-emotion-savee

# CONSTANT DECLARATION: emotion mapping according to SAVEE documentation
SAVEE_MAP = {
    "a": "angry",
    "d": "disgust",
    "f": "fearful",
    "h": "happy",
    "n": "neutral",
    "sa": "sad",
    "su": "surprised",
}


def load_savee(path):
    """
    @brief
        Loads & parses audio files from the SAVEE dataset directory.

    @param
        path: str
            Path to SAVEE folder w/ WAV files.

    @return
        ist[dict]
            A list of sample dictionaries of the form:
            {
                "path": "<absolute/path/to/file.wav>",
                "label": "<emotion>"
            }
    """
    # VARIABLE DECLARATION: list to accumulate parsed audio samples
    samples = []

    # PROCESS: recursively walking through SAVEE directory structure
    for file in os.listdir(path):

        if not file.endswith(".wav"):  # skipping non-WAV files
            continue

        name = file.lower()

        # PROCESS: parsing metadata from filename (refer to SAVEE doc for info)
        if name[3:5] in (
            "sa",
            "su",
        ):  # order matters: checking 2-char codes first (sa, su)
            emotion_label = SAVEE_MAP.get(name[3:5])
        else:
            emotion_label = SAVEE_MAP.get(name[3])

        if emotion_label:
            # PROCESS: appending parsed sample entry
            samples.append(
                {
                    "path": os.path.join(path, file),
                    "label": emotion_label,
                }
            )

    # OUTPUT:
    return samples


# -------------------- MASTER LOADER --------------------
def load_all_datasets(root="data"):
    """
    @brief
        Aggregates all supported datasets into a unified sample list.

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
    crema_path = os.path.join(root, "cremad")
    emodb_path = os.path.join(root, "emodb")
    savee_path = os.path.join(root, "savee")

    # PROCESS: checking for RAVDESS dataset & loading if present
    if os.path.exists(ravdess_path):

        print("[INFO] Loading RAVDESS dataset...")  # debugging
        samples.extend(load_ravdess(ravdess_path))  # adding to list

    else:  # error-handling
        # OUTPUT:
        print("[WARNING] No RAVDESS folder found at:", ravdess_path)

    # PROCESS: checking for CREMA-D dataset & loading if present
    if os.path.exists(crema_path):

        print("[INFO] Loading CREMA-D...")  # debugging
        samples.extend(load_cremad(crema_path))  # adding to list

    else:  # error-handling
        # OUTPUT:
        print("[WARN] CREMA-D missing at:", crema_path)

    # PROCESS: checking for EmoDB dataset & loading if present
    if os.path.exists(emodb_path):

        print("[INFO] Loading EmoDB...")  # debugging
        samples.extend(load_emodb(emodb_path))  # adding to list

    else:  # error-handling
        # OUTPUT:
        print("[WARN] EmoDB missing at:", emodb_path)

    # PROCESS: checking for SAVEE dataset & loading if present
    if os.path.exists(savee_path):

        print("[INFO] Loading SAVEE...")  # debugging
        samples.extend(load_savee(savee_path))  # adding to list

    else:  # error-handling
        # OUTPUT:
        print("[WARN] SAVEE missing at:", savee_path)

    # OUTPUT: aggregated dataset samples
    print(f"[INFO] Total samples loaded: {len(samples)}")
    return samples
