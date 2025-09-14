package com.example.kramelix;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    // private variables for recording and playing audio
    private ToggleButton recordButton, playRecButton;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String audioSavePath = null;
    private boolean isRecording = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        // running python code
        Python py = Python.getInstance();
        PyObject module = py.getModule("whisper");
        PyObject caller = module.get("test_function");

        System.out.println("3+1 = " + caller.call(3));
        System.out.println("hello?");



        // ----------------------------------------------- RECORDING BUTTON -----------------------------------------------
        recordButton = findViewById(R.id.recordButton);

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {

                // RECORD BUTTON IS ON -> record audio
                if (recordButton.isChecked()){

                    // check if permission to record audio is granted
                    if (checkPermissions()) {

                        // path where to save audio recording
                        audioSavePath = getAudioPath();

                        // initialize media recorder
                        mediaRecorder = new MediaRecorder();
                        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                        mediaRecorder.setOutputFile(audioSavePath);

                        // try to record audio from phone
                        try {
                            mediaRecorder.prepare();
                            mediaRecorder.start();
                            isRecording = true;
                            recordButton.setChecked(true);
                            Toast.makeText(MainActivity.this, "Recording started", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            safeReleaseRecorder();
                            isRecording = false;
                            recordButton.setChecked(false);
                            throw new RuntimeException(e);
                        }

                    }

                    // else, request permission to record audio
                    else {
                        recordButton.setChecked(false);
                        ActivityCompat.requestPermissions(MainActivity.this, new String[] {
                                Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE
                        }, 1);
                    }
                }

                // RECORD BUTTON IS OFF -> stop recording audio
                else {
                    if (isRecording && mediaRecorder != null) {
                        try {
                            mediaRecorder.stop(); // stop recording audio
                        } catch (RuntimeException ignored) {
                        } finally {
                            safeReleaseRecorder();
                            isRecording = false;
                            recordButton.setChecked(false);
                            Log.d("MainActivity", "Audio saved at: " + audioSavePath); // log audio path
                            Toast.makeText(MainActivity.this, "Recording stopped" + audioSavePath, Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        recordButton.setChecked(false);
                    }
                }
            }
        });

        // --------------------------------------------- PLAY RECORDING BUTTON ---------------------------------------------
        playRecButton = findViewById(R.id.playRecButton);

        playRecButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // PLAY RECORDING BUTTON IS ON -> start playing recording
                if (playRecButton.isChecked()){

                    mediaPlayer = new MediaPlayer();

                    // play recorded audio
                    try {
                        mediaPlayer.setDataSource(audioSavePath);
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                        Toast.makeText(MainActivity.this, "Started Playing Recording", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                // PLAY RECORDING IS OFF -> stop playing recording
                else {
                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        Toast.makeText(MainActivity.this, "Stopped Playing Recording", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        }

    // -------------------------------------------------- HELPER METHODS --------------------------------------------------

    /**
     * Private method to find the path where the recording is saved to once recorded
     * @return String representing the audio path location
     */
    private String getAudioPath() {
        return new java.io.File(
                getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC),
                "recording.m4a"
        ).getAbsolutePath();
    }

    /**
     * Private method to release the recorder when not in use
     */
    private void safeReleaseRecorder() {
        try { mediaRecorder.release(); } catch (Exception ignored) {}
        mediaRecorder = null;
    }

    /**
     * Private method to check if permission to record audio is granted
     * @return true if granted, false if not
     */
    private boolean checkPermissions() {
        int permission1 = ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.RECORD_AUDIO);

        return permission1 == PackageManager.PERMISSION_GRANTED;
    }
}