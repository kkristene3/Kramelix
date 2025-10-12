/**
 * Provides the Java–native interface layer for the Whisper speech-to-text engine.
 * <p>
 * This package contains a thin Java wrapper around the Whisper JNI bridge,
 * allowing our Android application to call native C++ functions for model loading
 * and audio transcription. It serves as the boundary between the managed Java layer
 * and the unmanaged native layer where the Whisper model runs.
 * </p>
 *
 * @since 1.0
 * @author Amy Huang
 * @version 1.0
 */
package com.example.kramelix.whisperjni;
