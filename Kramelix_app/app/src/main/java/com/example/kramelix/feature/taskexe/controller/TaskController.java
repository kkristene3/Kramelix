package com.example.kramelix.feature.taskexe.controller;

import java.lang.System;
public class TaskController {

    /**
     * This function identifies a task given by the user and calls the function required to perform it
     *
     * @param task String representing the task that needs to be performed.
     * @return Boolean representing the status of the task's completion
     * */
    public boolean executeTask(String task){
        //Play music task
        if (task.contains("playMusic(")){
            //extract the parameters from the string
            String[] taskParams = getParams(task);
            //if we found a song and artist in the params, then we call that version of the function
            if (taskParams.length == 2){
                return playMusic(taskParams[0], taskParams[1]);
            }
            //if only a song was found, then we call the song only playMusic function
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

        //if none of the above was fulfilled, that means that a task was not requested
        return true;
    }

    private String[] getParams(String text){
        //get the parameters between the ()
        String taskParams = text.substring(text.indexOf("(")+1, text.length()-2);
        //separate the parameters into an array using the , delimiter
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
        return true;
    }

    /**
     * Function for playing a song without a known artist
     *
     * @param song The song that the user wants to play
     * @return Boolean representing the success of the music playing*/
    private boolean playMusic(String song){
        System.out.println("Song: "+song);
        return true;
    }
}
