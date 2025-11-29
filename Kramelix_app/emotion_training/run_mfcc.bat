@echo off
for /f "delims=" %%a in ('echo prompt $E^| cmd') do set "ESC=%%a"

REM This batch file runs the full SER pipeline:
REM 1. Model training
REM 2. ONNX exports
REM 3. Plot training curves
REM 4. Build confusion matrices
REM Author: Amy Huang
REM Since: 1.0

echo AUDIO EMOTION MODEL TRAINING and EXPORT PIPELINE
echo ------------------------------------------------

REM --- CHECK PYTHON ---
python --version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo %ESC%[91mERROR: Python is not installed or not on PATH.%ESC%[0m
    pause
    exit /b 1
)

REM --- CHECK VENV EXISTS ---
IF NOT EXIST venv\Scripts\activate.bat (
    echo %ESC%[91mERROR: venv not found! Expected: venv\Scripts\activate.bat%ESC%[0m
    pause
    exit /b 1
)

REM --- CHECK TRAINING SCRIPT EXISTS ---
IF NOT EXIST run_training_mfcc.py (
    echo %ESC%[91mERROR: Missing: run_training_mfcc.py%ESC%[0m
    pause
    exit /b 1
)

REM --- CHECK EXPORT SCRIPT EXISTS ---
IF NOT EXIST run_export_mfcc.py (
    echo %ESC%[91mERROR: Missing: run_export_mfcc.py%ESC%[0m
    pause
    exit /b 1
)

REM --- MFCC PIPELINE ---
echo.
echo %ESC%[92m[1/5] Activating virtual environment...%ESC%[0m
call venv\Scripts\activate.bat

echo %ESC%[92m[2/5] Running training...%ESC%[0m
python run_training_mfcc.py
IF %ERRORLEVEL% NEQ 0 (
    echo %ESC%[91mERROR: MFCC training failed!%ESC%[0m
    pause
    exit /b 1
)

echo %ESC%[92m[3/5] Exporting MFCC ONNX model...%ESC%[0m
python run_export_mfcc.py
IF %ERRORLEVEL% NEQ 0 (
    echo %ESC%[91mERROR: MFCC export failed!%ESC%[0m
    pause
    exit /b 1
)

echo.
echo %ESC%[92mSUCCESS! Training and export complete.%ESC%[0m
echo Models saved under: models/audio_emotion_mfcc.onnx
echo ------------------------------------------------

echo.
echo Press ENTER to plot training curves...
pause >nul

echo %ESC%[92m[4/5] Plotting training curves...%ESC%[0m
python -m analysis.mfcc_archive.plot_training_curves_mfcc

echo.
echo Press ENTER to generate confusion matrix...
pause >nul

echo %ESC%[92m[5/5] Generating confusion matrix...%ESC%[0m
python -m analysis.mfcc_archive.confusion_matrix_mfcc

echo.
echo %ESC%[92mALL MFCC TASKS COMPLETE!%ESC%[0m
echo ------------------------------------------------
pause
