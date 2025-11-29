"""
This module loads all supported emotion-speech datasets from the `data/` directory
& normalizes their labels to a unified emotion vocabulary.

Datasets supported:
1. RAVDESS
2. CREMA-D
3. TESS (Toronto Emotional Speech Set)
4. SAVEE
5. EmoDB (German Emotional Speech Database)
6. IEMOCAP

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
# DATASET LINK: https://www.kaggle.com/datasets/piyushagni5/berlin-database-of-emotional-speech-emodb

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

        # VARIABLE DECLARATION: parsing metadata from file name (refer to EmoDB doc for info)
        if len(file) < 6:  # invalid file name
            continue  # skipping

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


# ---------------------------------------- TESS ----------------------------------------
# DATASET LINK: https://www.kaggle.com/datasets/ejlok1/toronto-emotional-speech-set-tess

TESS_MAP = {
    "angry": "angry",
    "disgust": "disgust",
    "fear": "fearful",
    "fearful": "fearful",
    "happy": "happy",
    "sad": "sad",
    "neutral": "neutral",
    "ps": "surprised",  # pleasant surprise
    "surprise": "surprised",
    "surprised": "surprised",
}


def load_tess(path):
    """
    @brief
        Loads & parses audio files from the TESS dataset directory.

    @param
        path: str
            Path to the TESS root folder containing WAV files.

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

    # PROCESS: iterating over TESS WAV files
    for file in os.listdir(path):

        if not file.endswith(".wav"):
            continue

        stem = os.path.splitext(file)[0].lower()
        parts = stem.split("_")

        # VARIABLE DECLARATION: emotion token is last segment (e.g. "angry", "sad", "ps")
        emotion_key = parts[-1]
        emotion_label = TESS_MAP.get(emotion_key)

        if emotion_label is None:  # unsupported/unknown emotion
            continue

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
        List[dict]
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
        if name[3:5] in ("sa", "su"):  # order matters: check 2-char codes first
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


# ---------------------------------------- IEMOCAP ----------------------------------------
# DATASET LINK: https://sail.usc.edu/iemocap/ (had to be requested)

IEMOCAP_MAP = {
    "neu": "neutral",
    "hap": "happy",
    "sad": "sad",
    "ang": "angry",
    "fear": "fearful",
    "sur": "surprised",
    "dis": "disgust",
}


def load_iemocap(path):
    """
    @brief
        Loads & parses audio files from the IEMOCAP dataset directory.

    @param
        path: str
            Path to IEMOCAP directory with .wav files & EmoEvaluation annotations.

    @return
        List[dict]
            A list of sample dictionaries of the form:
            {
                "path": "<absolute/path/to/dialog.wav>",
                "start": <float start time>,
                "end": <float end time>,
                "label": "<emotion>"
            }
    """

    # VARIABLE DECLARATION: list to accumulate parsed audio samples
    samples = []

    # PROCESS: recursively walking through IEMOCAP directory structure
    for session in os.listdir(path):

        # VARIABLE DECLARATION: extracting session directories
        session_dir = os.path.join(path, session)
        emo_eval = os.path.join(session_dir, "dialog/EmoEvaluation")
        wav_dir = os.path.join(session_dir, "dialog/wav")

        if not os.path.exists(emo_eval):
            continue

        # PROCESS: iterating through annotation files
        for ann_file in os.listdir(emo_eval):

            if not ann_file.endswith(".txt"):  # invalid file
                continue  # skipping

            ann_path = os.path.join(
                emo_eval, ann_file
            )  # extracting annotation file path

            with open(ann_path, "r", errors="ignore") as f:  # opening file

                # PROCESS: iterating through each line in annotation file
                for line in f:

                    if "[" not in line or "]" not in line:  # invalid line
                        continue  # skipping

                    # PROCESS: parsing timestamps (e.g. "[1.03 - 2.22]")
                    try:
                        ts_block = line.split("]")[0]  # e.g. "[1.03 - 2.22"
                        ts_block = ts_block.replace("[", "")  # e.g. "1.03 - 2.22"
                        start_str, end_str = ts_block.split("-")
                        start = float(start_str.strip())
                        end = float(end_str.strip())

                    except Exception:  # skipping malformed timestamp lines
                        continue

                    emotion = None  # temp var

                    # PROCESS: checking for parentheses-based emotion first
                    if "(" in line and ")" in line:  # found
                        inner = (
                            line.split("(")[1].split(")")[0].strip().lower()
                        )  # extracting

                        if inner in IEMOCAP_MAP:  # valid emotion
                            emotion = inner  # assigning

                    if emotion is None:  # need to try tab/space-based parsing instead

                        # PROCESS: splitting line into tokens
                        tokens = line.split()  # split on any whitespace

                        # PROCESS: iterating backwards to find last valid token
                        for tok in reversed(tokens):
                            tok = tok.lower().strip("(),")  # cleaning

                            if tok in IEMOCAP_MAP:  # valid emotion
                                emotion = tok  # assigning
                                break

                    if emotion is None:  # unsupported emotion
                        continue  # skipping

                    # VARIABLE DECLARATION: deriving wav file name (same prefix)
                    wav_name = ann_file.replace(".txt", ".wav")
                    wav_path = os.path.join(wav_dir, wav_name)

                    if not os.path.exists(wav_path):  # skipping mismatched files
                        continue

                    # PROCESS: appending parsed sample entry
                    samples.append(
                        {
                            "path": wav_path,
                            "start": start,
                            "end": end,
                            "label": IEMOCAP_MAP[emotion],
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
    tess_path = os.path.join(root, "tess")
    iemocap_path = os.path.join(root, "iemocap")

    # PROCESS: checking for RAVDESS dataset & loading if present
    if os.path.exists(ravdess_path):

        print("[INFO] Loading RAVDESS dataset...")  # debugging
        samples.extend(load_ravdess(ravdess_path))  # adding to list

    else:  # error-handling
        # OUTPUT:
        print("[WARNING] RAVDESS missing at:", ravdess_path)

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

    # PROCESS: checking for TESS dataset & loading if present
    if os.path.exists(tess_path):

        print("[INFO] Loading TESS...")  # debugging
        samples.extend(load_tess(tess_path))

    else:  # error-handling
        # OUTPUT:
        print("[WARN] TESS missing at:", tess_path)

    # PROCESS: checking for IEMOCAP dataset & loading if present
    if os.path.exists(iemocap_path):

        print("[INFO] Loading IEMOCAP...")  # debugging
        samples.extend(load_iemocap(iemocap_path))

    else:  # error-handling
        # OUTPUT:
        print("[INFO] IEMOCAP missing at:", iemocap_path)

    # OUTPUT: aggregated dataset samples
    print(f"[INFO] Total samples loaded: {len(samples)}")
    return samples
