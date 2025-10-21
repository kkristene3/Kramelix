package com.example.kramelix.feature.taskexe.controller;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import java.lang.System;

public class TaskController {

    // -------------------- STATE --------------------
    private static TaskController instance;
    private final Context context;

    // -------------------- PLAY MUSIC VARIABLE --------------------
    private MediaPlayer mediaPlayer;

    // ------------------------- LIFECYCLE -------------------------
    /**
     * Constructor
     *
     * @param context Any valid {@link Context}; the application context will be retained.
     */
    public TaskController(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * The public "gateway" to access the TaskController object
     * Creates a new object on the first call, afterwards, the pre-existing instance is returned
     *
     * @param context Any valid {@link Context}; the application context will be retained.
     * @return the shared TaskController instance
     */
    public static synchronized TaskController getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new TaskController(context);
        }
        return instance;
    }

    // ----------------------- EXECUTING TASKS -----------------------

    /**
     * This function identifies a task given by the user and calls the function required to perform it
     *
     * @param task String representing the task that needs to be performed.
     * @return Boolean representing the status of the task's completion
     * */
    public boolean executeTask(String task){

        // Task: Play Music
        if (task.contains("playMusic(")){

            // extract the parameters from the string
            String[] taskParams = getParams(task);

            // if we found a song and artist in the params, then we call that version of the function
            if (taskParams.length == 2){
                return playMusic(taskParams[0], taskParams[1]);
            }
            // if only a song was found, then we call the song only playMusic function
            else if (taskParams.length == 1){
                return playMusic(taskParams[0]);
            }
        }

        // Task: Set an Alarm
        else if (task.contains("setAlarm(")){

            // extract the parameters from the string
            String[] taskParams = getParams(task);

            // TODO: currently only accepts time in minutes,
            //  a user may want to set a timer using hours, or at an exact time; will have to adjust
            if (taskParams.length == 1){
                return setAlarm(taskParams[0]);
            }

        }

        /*else if (task.contains("call(")){

        }

        else if (task.contains("text(")){

        }

        else if (task.contains("searchUp(")){

        }

        else if (task.contains("openCamera")){
            System.out.println("openCamera");
        }*/

        // if none of the above was fulfilled, that means that a task was not requested
        return true;
    }

    // ----------------------- TASK PARAMETER -----------------------
    private String[] getParams(String text){
        // get the parameters between the ()
        String taskParams = text.substring(text.indexOf("(")+1, text.length()-2);

        // separate the parameters into an array using the , delimiter
        String[] params = taskParams.split(",");

        return params;
    }

    // ---------------------- TASK: PLAY MUSIC -----------------------

    /**
     * Function for playing a song
     *
     * @param song The song that the user wants to play
     * @param artist The artist of the given song
     * @return Boolean representing the success of the music playing. Success = true, any error (including unable to find song) is false*/
    private boolean playMusic(String song, String artist){
        System.out.println("Song: "+song+", Artist: "+artist);
        return playMusic(song);
    }

    /**
     * Function for playing a song without a known artist
     *
     * @param song The song that the user wants to play
     * @return Boolean representing the success of the music playing
     * */
    private boolean playMusic(String song){

        try {
            // PROCESS: clean previous MediaPlayer
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            // PROCESS: ensure song name is readable by AndroidStudio
            String songName = song.trim().toLowerCase().replace(" ", "_");

            // PROCESS: get resource ID (music file)
            int resId = context.getResources().getIdentifier(songName, "raw", context.getPackageName());

            // PROCESS: if cannot find song in folder
            if (resId == 0) {
                System.out.println("TaskController - Song not found in res/raw: " + songName);
                return false;
            }

            // PROCESS: create and start music player
            mediaPlayer = MediaPlayer.create(context, resId);
            mediaPlayer.start();

            // PROCESS: print result in logs
            System.out.println("TaskController - Playing: " + songName);
            return true;

        } catch (Exception e) { // error-handling
            System.out.println("TaskController - Failed to play music: " + e);
            return false;
        }
    }

    /**
     * Function to stop playing music
     */
    public void stopMusic() {
        // PROCESS: stop playing music if llm is currently playing music
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            System.out.println("TaskController - Music stopped");
        }
    }

    // ----------------------- TASK: SET ALARM -----------------------

    /**
     * Function that sets an alarm based on the time (minutes) given by user
     *
     * @param inputStr String representing for how long, in minutes, to set the alarm for
     */
    private boolean setAlarm(String inputStr){
        try {

            // PROCESS: convert input string to int
            String timeStr = inputStr.replaceAll("[^0-9]", ""); // keeps digits only
            int minutes = Integer.parseInt(timeStr);

            // PROCESS: calculate trigger time in milliseconds
            long triggerAtMillis = SystemClock.elapsedRealtime() + (long) minutes * 60 * 1000;

            // PROCESS: create alarm
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiver.class);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // PROCESS: set the alarm to go off at an exact time, regardless if phone is in low-power "idle" or "doze" mode.
            // Check the Android version of device and set the alarm appropriately
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) { // for devices running Android's current version
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        alarmIntent
                );
            } else { // for devices running old versions of Android
                alarmManager.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        alarmIntent
                );
            }

            // OUTPUT: show result in log, return true if able to set alarm
            System.out.println("TaskController - Alarm set for " + minutes + " minutes from now");
            return true;

        } catch (Exception e) { // error-handling
            System.out.println("TaskController - Failed to set alarm: " + e);
            return false;
        }
    }
}
