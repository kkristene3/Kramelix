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

_AMY'S NOTE: both models take around 30 to 50 minutes to train._

How to run server.py:
In network_security_config.xml: If not already done, change `YOUR IP` to your actual IP and add your IP to your local.properties (save it as SERVER_URL)
In the root folder (`Kramelix\Kramelix_app\emotion_training`), run:
1. `py -3.10 -m venv venv` (creating a virtual environment for Python 3.10)
2. `venv\Scripts\activate` (entering the venv)
3. `pip install --upgrade pip`
4. `pip install -r requirements.txt`
5. `python -m pip install fastapi uvicorn[standard] onnxruntime librosa soundfile numpy` (I will get rid of repeats and add these to requirements.txt soon)
6. `python -m uvicorn server:app --host 0.0.0.0 --port 8000`
