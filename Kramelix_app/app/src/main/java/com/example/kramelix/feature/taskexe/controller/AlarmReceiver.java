package com.example.kramelix.feature.taskexe.controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;


/**
 * This receiver is a BroadcastReceiver and is triggered by the system AlarmManager.
 * It performs a scheduled task in delivering a "time-up" message when an alarm is to go off.
 *
 * @author Kristen Duong
 * @noinspection PublicConstructor
 * @since 1.0
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public final void onReceive(Context context, Intent intent) {
        try {

            // PROCESS: get location of device's ringtone
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (null == alarmUri) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); // use the default notification sound if a ringtone does not exist
            }

            // PROCESS: play the ringtone when alarm goes off
            Ringtone ringtone = RingtoneManager.getRingtone(context, alarmUri);
            ringtone.play();

            // Toast output
            Toast.makeText(context, "Your alarm has been triggered!", Toast.LENGTH_LONG).show();

            // logcat output
            System.out.println("Alarm triggered!");

            // stop the ringtone after 8 seconds
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                if (ringtone.isPlaying()) {
                    ringtone.stop();
                    System.out.println("Alarm stopped after 8 seconds");
                }
            }, 8000);

        } catch (RuntimeException e) { // error-handling
            // TODO: add better error-handling here
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

}
