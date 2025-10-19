package com.example.kramelix.feature.taskexe.controller;

import android.content.Context;
import android.media.MediaPlayer;

import androidx.annotation.NonNull;

import java.lang.System;

public class TaskController {

    private static TaskController instance;
    private MediaPlayer mediaPlayer;
    private final Context context;

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

    /**
     * This function identifies a task given by the user and calls the function required to perform it
     *
     * @param task String representing the task that needs to be performed.
     * @return Boolean representing the status of the task's completion
     * */
    public boolean executeTask(String task){
        // Play music task
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

        /*else if (task.contains("setAlarm(")){

        }

        else if (task.contains("call(")){

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

    private String[] getParams(String text){
        // get the parameters between the ()
        String taskParams = text.substring(text.indexOf("(")+1, text.length()-2);
        // separate the parameters into an array using the , delimiter
        String[] params = taskParams.split(",");

        return params;
    }

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
     * @return Boolean representing the success of the music playing*/
    private boolean playMusic(String song){

        try {
            // clean previous MediaPlayer
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            // ensure song name is readable by AndroidStudio
            String songName = song.trim().toLowerCase().replace(" ", "_");

            // get resource ID
            int resId = context.getResources().getIdentifier(songName, "raw", context.getPackageName());

            // if cannot find song in folder
            if (resId == 0) {
                System.out.println("TaskController - Song not found in res/raw: " + songName);
                return false;
            }

            // create and start music player
            mediaPlayer = MediaPlayer.create(context, resId);
            mediaPlayer.start();

            // result
            System.out.println("TaskController - Playing: " + songName);
            return true;

        } catch (Exception e) {
            System.out.println("Can't play music: " + e);
            return false;
        }
    }

    /**
     * Function to stop playing music
     */
    public void stopMusic() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            System.out.println("TaskController - Music stopped");
        }
    }
}
