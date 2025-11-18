package com.example.kramelix.feature.taskexe.controller;

import android.content.Context;
import android.content.Intent;

/**
 * This receiver is a BroadcastReceiver and is triggered by the system when a song starts playing
 * It creates a notification that will allow the use to play and pause
 *
 * @author Kristen Duong
 * @noinspection PublicConstructor
 * @since 1.0
 */

public class MediaActionReceiver extends android.content.BroadcastReceiver {

    @Override
    public final void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        TaskController controller = TaskController.getInstance(context);

        if ("ACTION_PLAY_PAUSE".equals(action)) {
            if (null != controller.mediaPlayer && controller.mediaPlayer.isPlaying()) {
                controller.pauseMusic();
            } else {
                controller.resumeMusic();
            }
            controller.showNotification(); // update notification
        }
    }
}
