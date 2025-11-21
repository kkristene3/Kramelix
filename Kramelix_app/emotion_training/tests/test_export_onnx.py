# run `python -m tests.test_export_onnx`

"""
This test module checks that the .h5 model converts nicely to an ONNX model.

Author: Amy Huang
Since: 1.0
"""

import numpy as np
import onnxruntime as ort

# VARIABLE DECLARATION: retrieving moddel
session = ort.InferenceSession("models/audio_emotion.onnx")

input_name = session.get_inputs()[0].name
dummy = np.random.rand(1, 40).astype(np.float32)
output = session.run(None, {input_name: dummy})

# OUTPUT: dumping probability of each emotion
print(output)
