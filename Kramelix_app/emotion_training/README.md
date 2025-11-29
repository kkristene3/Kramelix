# How to Run Test Scripts:

**REQUIRED**: Python 3.10.xx & all setup in SETUP.md.

In the root folder (`Kramelix\Kramelix_app\emotion_training`), run:
`python -m tests.[folder_name].[script_name]` (e.g.
`python -m tests.mfcc_archive.test_sample_data_mfcc`)

_NOTE: (so far) only `test_sample_data.py` requires actual test data._

# How to Run Analysis Scripts:

**REQUIRED**: Python 3.10.xx & all setup in SETUP.md.

In the root folder (`Kramelix\Kramelix_app\emotion_training`), run:
`python -m analysis.[folder_name].[script_name]` (e.g.
`python -m analysis.mfcc_archive.plot_training_curves_mfcc`)
