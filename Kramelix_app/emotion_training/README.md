# 📋 Organizational Overview:

Currently, we have two models: MFCC (the old implementation; way overfitted & less accurate) and
Melspectrogram CNN (more accurate).

The models are trained on six datasets: RAVDESS, SAVESS, TESS, CREMA-D, EmoDB, and IEMOCAP (total of
approx. 15k .WAV files).
The `Kramelix\Kramelix_app\emotion_training\data` folder is empty in our repository because we don't
need to commit 29 GB of data here.

## 🎯 How to Run Analysis Scripts:

**REQUIRED**: Python 3.10.xx + all setup in SETUP.md + fully trained model(s).

In the root folder (`Kramelix\Kramelix_app\emotion_training`), run:
`python -m analysis.[folder_name].[script_name]` (e.g.
`python -m analysis.mfcc_archive.plot_training_curves_mfcc`)

## 📝 How to Run Test Scripts:

**REQUIRED**: Python 3.10.xx & all setup in SETUP.md.

In the root folder (`Kramelix\Kramelix_app\emotion_training`), run:
`python -m tests.[folder_name].[script_name]` (e.g.
`python -m tests.mfcc_archive.test_sample_data_mfcc`)

_NOTE: (so far) only `test_sample_data_mfcc.py` and `test_sample_data_melspec.py` requires actual
test data._

## 🗺️ Real-World Tests:

In the test folder, we can import our own .WAV files locally to
`Kramelix\Kramelix_app\emotion_training\tests\test_data` to see how our models perform on real-world
data.

_AMY'S NOTE: DO NOT COMMIT TEST DATA, OR ELSE, WE RISK BLOATING OUR REPO._
