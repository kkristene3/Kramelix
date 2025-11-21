@echo off
color 0A

echo ================================================
echo   AUDIO EMOTION MODEL: TRAIN + EXPORT PIPELINE
echo ================================================

REM --- CHECK PYTHON ---
python --version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo ERROR: Python is not installed or not on PATH.
    pause
    exit /b 1
)

REM --- CHECK VENV EXISTS ---
IF NOT EXIST venv\Scripts\activate.bat (
    echo ERROR: venv not found! Expected: venv\Scripts\activate.bat
    pause
    exit /b 1
)

REM --- CHECK TRAINING SCRIPT EXISTS ---
IF NOT EXIST run_training.py (
    echo ERROR: Missing: run_training.py
    pause
    exit /b 1
)

REM --- CHECK EXPORT SCRIPT EXISTS ---
IF NOT EXIST run_export.py (
    echo ERROR: Missing: run_export.py
    pause
    exit /b 1
)

echo.
echo [1/3] Activating virtual environment...
call venv\Scripts\activate.bat

echo [2/3] Running training...
python run_training.py
IF %ERRORLEVEL% NEQ 0 (
    echo ERROR: Training failed!
    pause
    exit /b 1
)

echo [3/3] Exporting ONNX model...
python run_export.py
IF %ERRORLEVEL% NEQ 0 (
    echo ERROR: Export failed!
    pause
    exit /b 1
)

echo.
echo SUCCESS! Pipeline complete.
echo Models saved under: models\
echo ----------------------------------------
pause
