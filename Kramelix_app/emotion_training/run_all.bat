@echo off
REM This batch file runs the model train (`run_training.py`) and exports the Keras model to an ONNX model (`run_export.py`).
REM Author: Amy Huang
REM Since: 1.0

echo AUDIO EMOTION MODEL TRAINING and EXPORT PIPELINE
echo ------------------------------------------------

echo Activating virtual environment...
call venv\Scripts\activate.bat

echo Running training...
python run_training.py
IF %ERRORLEVEL% NEQ 0 (
    echo Training failed!
    exit /b 1
)

echo Running ONNX export...
python run_export.py
IF %ERRORLEVEL% NEQ 0 (
    echo Export failed!
    exit /b 1
)

echo.
echo Pipeline complete!
pause
