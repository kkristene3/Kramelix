# Setup:

**REQUIRED**: Python 3.10.xx (install this before doing any of the steps below)

In the root folder (`Kramelix\Kramelix_app\emotion_training`), run:

1. `py -3.10 -m venv venv` (creating a virtual environment for Python 3.10)
2. `venv\Scripts\activate` (entering the venv)
3. `pip install --upgrade pip`
4. `pip install -r requirements.txt`

_NOTE: you can skip steps 1–2 if you only have Python 3.10 (which is highly unlikely since we use
3.13 elsewhere)._

# How to Run Model Training:

In the root folder, run: `.\run_mfcc.bat` or `.\run_melspec.bat` (or just click on the batch file
from Windows File
Explorer)

_AMY'S NOTE: both models take around 30 to 40 minutes to train._
