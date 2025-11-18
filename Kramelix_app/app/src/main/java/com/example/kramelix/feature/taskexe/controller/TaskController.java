package com.example.kramelix.feature.taskexe.controller;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.kramelix.R;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * This controller executes user-requested device actions recognized by the LLM.
 *
 * <p>It exposes a simple string-based task execution API, allowing higher-level logic
 * (e.g. the transcription + LLM pipeline) to trigger actions such as playing music or
 * setting an alarm. The controller acts as an abstraction over Android system services
 * that must be initialized with a {@link Context}.</p>
 *
 * <br>
 * <strong>Responsibilities</strong>
 * <ul>
 *     <li>Owns a lightweight registry of supported tasks (e.g. music playback, alarms).</li>
 *     <li>Parses task command strings and dispatch to the correct implementation.</li>
 *     <li>Manages system resources such as {@link android.media.MediaPlayer} safely.</li>
 *     <li>Serves as the feature-level extension point for future "assistant-like" abilities.</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>This class is a per-process singleton; access is via {@link #getInstance(Context)}.</li>
 *     <li>Execution methods must never block the UI thread for long operations.</li>
 *     <li>All task command formats must be explicitly validated and handled defensively.</li>
 *     <li>Unknown or unsupported task strings must fail gracefully without side effects.</li>
 * </ul>
 *
 * @author Alex Oprea, Kristen Duong, Amy Huang
 * @noinspection BooleanMethodNameMustStartWithQuestion, Singleton
 * @since 1.0
 */
public final class TaskController {

    // -------------------- STATE --------------------
    private static TaskController instance;
    private final Context context;

    // -------------------- PLAY MUSIC VARIABLE --------------------
    MediaPlayer mediaPlayer;
    private int pausedTime;
    private MediaSessionCompat mediaSession;
    private String songReadableName;
    private String songArtist;

    // -------------------- NOTIFICATION CHANNEL --------------------
    private static final String CHANNEL_ID = "Media Channel";
    private NotificationManager notificationManager;

    // ----------------------------- UI -----------------------------
    private LinearLayout musicControlsLayout;
    private ImageButton playResumeMusicButton;
    /**
     * @noinspection WeakerAccess
     */
    TextView alarmCountdownText;

    // ----------------------------- LIFECYCLE -----------------------------

    /**
     * Constructor
     *
     * @param context Current valid {@link Context}.
     */
    private TaskController(@NonNull Context context) {
        this.context = context.getApplicationContext();
        initMediaSession();
        createNotificationChannel();
        // AMY'S NOTE: keep context as-is!!! DON'T convert to App context
        //THe app doesn't run if it's kept as is, it gets an error and quits immediately
        //this.context = context;
    }

    /**
     * The public "gateway" to access the TaskController object
     * Creates a new object on the first call, afterwards, the pre-existing instance is returned
     *
     * @param context Current valid {@link Context}.
     * @return the shared TaskController instance
     */
    public static synchronized TaskController getInstance(@NonNull Context context) {
        if (null == instance) {
            instance = new TaskController(context);
        }
        //noinspection StaticVariableUsedBeforeInitialization
        return instance;
    }

    /**
     * Set up a Media Session to handle music playback in notification
     */
    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(context, "TaskController");

        // PROCESS: set initial playback state with actions that will be used
        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_STOP |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
                .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1.0f)
                .build();
        mediaSession.setPlaybackState(state);

        // PROCESS: receives media buttons, transport controls, and commands
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                resumeMusic();
            }

            @Override
            public void onPause() {
                pauseMusic();
            }

            @Override
            public void onStop() { // TODO: remove later?
                stopMusic();
            }
        });

        mediaSession.setActive(true); // indicate session is active
    }

    /**
     * Associate the music controls layout with a Linear Layout container
     * Associate the play/resume button with an Image button
     * Associate the alarm countdown time with a TextView
     *
     * @param layout-               the Linear Layout containing the music control buttons;
     *                              received from MainActivity.java
     * @param playResumeMusicButton - the Image Button a user clicks to pause/resume music
     * @param alarmCountdownText    - the TextView showing the countdown for the alarm
     */
    public void setUIElements(LinearLayout layout, ImageButton playResumeMusicButton, TextView alarmCountdownText) {
        musicControlsLayout = layout;
        this.playResumeMusicButton = playResumeMusicButton;
        this.alarmCountdownText = alarmCountdownText;
    }

    // ----------------------- EXECUTING TASKS -----------------------

    /**
     * This function identifies a task given by the user and calls the function required to perform it
     *
     * @param task String representing the task that needs to be performed.
     * @return Boolean representing the status of the task's completion
     */
    public boolean executeTask(String task) {

        // Task: Play Music
        if (task.contains("playMusic(")) {

            // extract the parameters from the string
            String[] taskParams = getParams(task);

            // if we found a song and artist in the params, then we call that version of the function
            if (2 == taskParams.length) {
                return playMusic(taskParams[0], taskParams[1]);
            }
            // if only a song was found, then we call the song only playMusic function
            else if (1 == taskParams.length) {
                return playMusic(taskParams[0]);
            }
        }

        //Task: Play Music based on an Artist
        else if (task.contains("playMusicArtist(")) {
            String[] taskParams = getParams(task);
            if (1 == taskParams.length) {
                return playMusicArtist(taskParams[0]);
            }
        }

        //Task: Play Music based on a Genre
        else if (task.contains("playMusicGenre(")) {
            String[] taskParams = getParams(task);
            if (1 == taskParams.length) {
                return playMusicGenre(taskParams[0]);
            }
        } else if (task.contains("setAlarm(")) { // task: Set an Alarm

            // extract the parameters from the string
            String[] taskParams = getParams(task);

            // TODO: currently only accepts time in minutes,
            //  a user may want to set a timer using hours, or at an exact time; will have to adjust
            if (1 == taskParams.length) {
                return setAlarm(taskParams[0]);
            }

        } else if (task.contains("call(")) { // task: call

            // VARIABLE DECLARATION: extracting the parameters from the string
            String[] params = getParams(task);

            if (1 == params.length) {

                // PROCESS: trimming the request & sending to CallController
                String target = params[0].trim();
                CallController callController = CallController.getInstance(context);

                return callController.handleCallRequest(target);

            }

        } else if (task.contains("confirm(")) { // task: confirm a contact name

            // VARIABLE DECLARATION: extracting the parameters from the string
            String[] params = getParams(task);

            if (1 == params.length) {

                // PROCESS: trimming the request
                String ambiguous = params[0].trim();

                System.out.println("TaskController - Confirmation needed for: " + ambiguous);

                // OUTPUT: intentionally NOT calling anyone here; UI will prompt the user again
                return true;

            }

        }

        // if none of the above was fulfilled, that means that a task was not requested
        return true;
    }

    // ----------------------- TASK PARAMETER -----------------------
    private static String[] getParams(String text) {
        // get the parameters between the ()
        String taskParams = text.substring(text.indexOf('(') + 1, text.length() - 2);

        // separate the parameters into an array using the , delimiter
        return taskParams.split(",");
    }

    // ---------------------- TASK: PLAY MUSIC -----------------------

    /**
     * Function for playing a song
     *
     * @param song   The song that the user wants to play
     * @param artist The artist of the given song
     * @return Boolean representing the success of the music playing. Success = true, any error (including unable to find song) is false
     */
    private boolean playMusic(String song, String artist) {
        System.out.println("Song: " + song + ", Artist: " + artist);

        // PROCESS: ensure song name is readable by AndroidStudio
        String tempSongName = song.trim().toLowerCase().replace(" ", "_");

        // PROCESS: get resource ID (music file)
        @SuppressLint("DiscouragedApi") int resId = context.getResources().getIdentifier(tempSongName, "raw", context.getPackageName());

        // PROCESS: if cannot find song in folder
        if (0 == resId) {
            System.out.println("TaskController - Song not found in res/raw: " + tempSongName);
            return false;
        }
        //PROCESS: verify if the artist is correct
        String artistMeta = getSongMetadata("Artist", resId);
        if (null != artistMeta && artistMeta.toLowerCase().contains(artist.toLowerCase().trim())) {
            return playMusic(song);
        }
        System.out.println("Song artist doesn't match given artist" + artist);
        return false;
    }

    /**
     * Function for playing a song without a known artist
     *
     * @param song The song that the user wants to play
     * @return Boolean representing the success of the music playing
     */
    private boolean playMusic(String song) {

        try {
            // PROCESS: clean previous MediaPlayer
            if (null != mediaPlayer) {
                mediaPlayer.release();
            }

            // PROCESS: ensure song name is readable by AndroidStudio
            String songName = song.trim().toLowerCase().replace(" ", "_");

            // PROCESS: get resource ID (music file)
            @SuppressLint("DiscouragedApi") int resId = context.getResources().getIdentifier(songName, "raw", context.getPackageName());

            // PROCESS: if cannot find song in folder
            if (0 == resId) {
                System.out.println("TaskController - Song not found in res/raw: " + songName);
                return false;
            }

            // PROCESS: Get the readable song title
            songReadableName = getSongMetadata("Title", resId);
            if (null == songReadableName || song.isEmpty()) {
                songReadableName = songName;
            }

            songArtist = getSongMetadata("Artist", resId);
            if (null == songArtist || songArtist.isEmpty()) {
                songArtist = "Unknown";
            }

            // PROCESS: create and start music player
            mediaPlayer = MediaPlayer.create(context, resId);
            mediaPlayer.setOnCompletionListener(mp -> stopMusic());
            mediaPlayer.start();

            // update UI -> show music controls
            showMusicControls();

            // PROCESS: update playback state
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);

            // TODO: can update metadata here -> using MediaMetadataCompat

            // PROCESS: trigger notification
            showNotification();

            // PROCESS: print result in logs
            System.out.println("TaskController - Playing: " + songName);
            return true;

        } catch (IllegalStateException e) {
            System.out.println("TaskController - Illegal state, failed to play music: " + e);
            return false;
        } catch (RuntimeException e) { // error-handling
            System.out.println("TaskController - Runtime, failed to play music: " + e);
            return false;
        }
    }

    /**
     * Function for playing a song based on a given artist
     *
     * @param artist The artist of the song to play
     * @return Boolean representing the success of the music playing. Success = true, any error (including unable to find song) is false
     */
    private boolean playMusicArtist(String artist) {
        String song = getMusicBasedOnMetadata("Artist", artist);
        if (null != song) {
            return playMusic(song);
        }
        //TODO error handle the songs we can't find =(
        else {
            System.out.println("couldn't find song =(");
            return false;
        }
    }

    /**
     * Function for playing a song based on a given genre
     *
     * @param genre The genre of the song to play
     * @return Boolean representing the success of the music playing. Success = true, any error (including unable to find song) is false
     */
    private boolean playMusicGenre(String genre) {
        String song = getMusicBasedOnMetadata("Genre", genre);
        if (null != song) {
            return playMusic(song);
        }
        //TODO error handle the songs we can't find =(
        else {
            System.out.println("couldn't find song =(");
            return false;
        }
    }

    /**
     * Function that chooses a song based on a given metadata value
     *
     * @param type - The type of metadata to find and use ("Artist" or "Genre")
     * @param data - The data that we want to choose a song based off (Name of the artist or genre)
     * @return string containing the title of the chosen song
     */
    @Nullable
    private String getMusicBasedOnMetadata(String type, String data) {
        System.out.println("data:" + data);
        //list of matching songs
        List<String> songList = new ArrayList<>(0);

        List<Integer> musicIds = getAllSongResourceIds();

        for (int id : musicIds) {
            //create a new Metadata retriever
            MediaMetadataRetriever meta = new MediaMetadataRetriever();
            AssetFileDescriptor afd;
            String songTitle;

            try {
                afd = context.getResources().openRawResourceFd(id);
                songTitle = context.getResources().getResourceEntryName(id);
                if (null == afd) {
                    continue;
                }
                //set the metadata object to the current file that we want to examine
                meta.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());

                //look for any artists in the f
                if ("Artist".equals(type)) {
                    String artist = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

                    if (null != artist && artist.toLowerCase().contains(data.toLowerCase())) {
                        if (null != songTitle) {
                            songList.add(songTitle);
                        }
                    }
                }
                if ("Genre".equals(type)) {
                    String genre = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);

                    if (null != genre && genre.toLowerCase().contains(data.toLowerCase())) {
                        if (null != songTitle) {
                            songList.add(songTitle);
                        }
                    }
                }
                //close the created resources
                try {
                    meta.release();
                    afd.close();
                } catch (IOException e) {
                    System.out.println("TaskController - I/O, failed to close resources: " + e);
                } catch (RuntimeException e) {
                    System.out.println("TaskController - Runtime, failed to close resources: " + e);
                }
            } catch (Resources.NotFoundException e) {
                System.out.println("TaskController - FNFE, failed to find music file: " + e);
            } catch (IllegalArgumentException e) {
                System.out.println("TaskController - Illegal Arg, failed to find music file: " + e);
            } catch (RuntimeException e) {
                System.out.println("TaskController - Runtime, failed to find music file: " + e);
            }
        }
        //turn the song list into an array for easier indexing
        String[] iterableSongList = songList.toArray(new String[0]);

        //if there is only one song that fits the requirements, return that song
        if (1 == iterableSongList.length) {
            return iterableSongList[0];
        }
        //if there are no songs that fit the requirement, return null
        else if (0 == iterableSongList.length) {
            return null;
        }
        //if there are multiple songs that fit the requirement, we must choose a random song to return
        else {
            int randomNum = new SecureRandom().nextInt(iterableSongList.length);
            return iterableSongList[randomNum];
        }
    }

    /**
     * Function that returns the desired metadata for a specific mp3 file ID
     *
     * @param type - The type of metadata to grab (Artist or Title)
     * @param id   - The ID for the mp3 file of the chosen song
     * @return String - The metadata
     */
    @Nullable
    private String getSongMetadata(String type, int id) {
        //create a new Metadata retriever
        MediaMetadataRetriever meta = new MediaMetadataRetriever();
        AssetFileDescriptor afd;

        try {
            afd = context.getResources().openRawResourceFd(id);
            if (null == afd) {
                return null;
            }

            meta.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());

            if ("Artist".equals(type)) {
                return meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            } else if ("Title".equals(type)) {
                return meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            }

        } catch (Resources.NotFoundException e) {
            System.out.println("TaskController - FNFE, failed to find music file metadate: " + e);
        } catch (IllegalArgumentException e) {
            System.out.println("TaskController - Illegal Arg, failed to find music file metadate: " + e);
        } catch (RuntimeException e) {
            System.out.println("TaskController - Runtime, failed to find music file metadate: " + e);
        }
        return null;

    }

    /**
     * Get the song title, artist or genre from the music file's metadata
     *
     * @return List<Integer> containing the list of music file ids
     */
    private List<Integer> getAllSongResourceIds() {
        List<Integer> ids = new ArrayList<>(0);
        try {
            Class<?> raw = Class.forName(context.getPackageName() + ".R$raw");
            Field[] fields = raw.getDeclaredFields();
            for (Field field : fields) {
                ids.add(field.getInt(null));
            }
        } catch (ClassNotFoundException e) {
            System.out.println("TaskController - Class Not Found, failed to retrieve song resource IDs: " + e);
        } catch (IllegalAccessException e) {
            System.out.println("TaskController - Illegal Access, failed to retrieve song resource IDs (likely missing permission): " + e);
        } catch (IllegalArgumentException e) {
            System.out.println("TaskController - Illegal Arg, failed to retrieve song resource IDs: " + e);
        } catch (RuntimeException e) {
            System.out.println("TaskController - Runtime, failed to retrieve song resource IDs: " + e);
        }
        return ids;
    }

    /**
     * Function to pause music
     */
    public void pauseMusic() {
        if (isMusicPlaying()) {
            mediaPlayer.pause(); // pause song
            pausedTime = mediaPlayer.getCurrentPosition(); // get time of song it was paused at
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED); // update playback state
            switchPlayResumeMusicIcon(playResumeMusicButton); // update play/resume button
            System.out.println("TaskController - Music paused");
        }
    }

    /**
     * Function to resume music
     */
    public void resumeMusic() {
        if (null != mediaPlayer) {
            mediaPlayer.seekTo(pausedTime);
            mediaPlayer.start(); // start playing song from where it was paused
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING); // update playback state
            switchPlayResumeMusicIcon(playResumeMusicButton); // update play/resume button
            System.out.println("TaskController - Music resumed");
        }
    }

    /**
     * Function to stop playing music
     */
    public void stopMusic() {
        // PROCESS: stop playing music if llm is currently playing music
        if (null != mediaPlayer) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED); // update playback state
            hideMusicControls(); // update UI -> hide music controls
            System.out.println("TaskController - Music stopped");

            // remove notification
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(1);
        }
    }

    /**
     * This function will set the Playback state to the state given
     *
     * @param state - what playback state to set it to
     */
    private void updatePlaybackState(int state) {
        PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_STOP
                )
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f);
        mediaSession.setPlaybackState(builder.build());
    }

    // ----------------------- TASK: SET ALARM -----------------------

    /**
     * Function that sets an alarm based on the time (minutes) given by user
     *
     * @param inputStr String representing for how long, in minutes, to set the alarm for
     */
    private boolean setAlarm(String inputStr) {
        try {

            // PROCESS: convert input string to int
            // noinspection DynamicRegexReplaceableByCompiledPattern
            String timeStr = inputStr.replaceAll("[^0-9]", ""); // keeps digits only
            int minutes = Integer.parseInt(timeStr);

            // PROCESS: calculate trigger time in milliseconds
            long triggerAtMillis = SystemClock.elapsedRealtime() + (long) minutes * 60 * 1000;

            // PROCESS: create alarm
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiver.class);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // FIXME OPTIMIZE: could probably remove the if-block here bc the SDK_INT is always >= 24
            // PROCESS: set the alarm to go off at an exact time, regardless if phone is in low-power "idle" or "doze" mode.
            // Check the Android version of device and set the alarm appropriately
            if (android.os.Build.VERSION_CODES.M <= android.os.Build.VERSION.SDK_INT) { // for devices running Android's current version
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

            // show countdown
            showAlarmTime();

            // create countdown on main (UI) thread
            new android.os.Handler(context.getMainLooper()).post(() -> new CountDownTimer(minutes * 60L * 1000L, 1000) {
                @Override
                public void onFinish() {
                    hideAlarmtime(); // hide time once alarm triggers
                }

                @SuppressLint("DefaultLocale")
                @Override
                public void onTick(long l) { // keep counting down every second (where l is ms until finish)
                    long hour = (l / 3600000) % 24;
                    long min = (l / 60000) % 60;
                    long sec = (l / 1000) % 60;

                    if (null != alarmCountdownText) {
                        alarmCountdownText.setText(String.format("%02d:%02d:%02d", hour, min, sec));
                    }
                }
            }.start());

            // OUTPUT: show result in log, return true if able to set alarm
            System.out.println("TaskController - Alarm set for " + minutes + " minutes from now");
            return true;

        } catch (NumberFormatException e) {
            System.out.println("TaskController - Number format, failed to set alarm: " + e);
            return false;
        } catch (RuntimeException e) { // error-handling
            System.out.println("TaskController - Runtime, failed to set alarm: " + e);
            return false;
        }
    }

    // -------------------- NOTIFICATION CHANNEL --------------------

    /**
     * This function register the app's notification channel with the system
     */
    private void createNotificationChannel() {
        // PROCESS: create the notification channel
        if (android.os.Build.VERSION_CODES.O <= android.os.Build.VERSION.SDK_INT) {
            CharSequence name = "Media Playback";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * This function creates the notification to display
     */
    void showNotification() {

        // PROCESS: create a play/pause intent
        Intent playPauseIntent = new Intent(context, MediaActionReceiver.class)
                .setAction("ACTION_PLAY_PAUSE");
        PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // PROCESS: build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.kramelix_logo) // FIXME: this could later be changed to the song's album cover (if there's time)
                .setContentTitle(songReadableName)
                .setContentText(songArtist)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0)) // 0 = play/pause
                .addAction(new NotificationCompat.Action(
                        R.drawable.play, "Play/Pause", playPausePendingIntent // index 0
                ));

        // PROCESS: get the system's notification center and post the notification
        android.app.NotificationManager notificationService =
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationService.notify(1, builder.build());
    }

    // ------------------------------- HANDLE UI CHANGES -------------------------------

    /**
     * Set the layout to VISIBLE
     */
    private void showMusicControls() {
        if (null != musicControlsLayout) {
            // runs on the main thread
            new android.os.Handler(context.getMainLooper()).post(() -> musicControlsLayout.setVisibility(View.VISIBLE));
        }
    }

    /**
     * Set the layout to INVISIBLE
     */
    private void hideMusicControls() {
        if (null != musicControlsLayout) {
            // runs on the main thread
            new android.os.Handler(context.getMainLooper()).post(() -> musicControlsLayout.setVisibility(View.INVISIBLE));
        }
    }

    /**
     * Change the play/resume button icon based on whether music is being played or not
     *
     * @param button - the Image Button used to control playing and resuming music
     */
    private void switchPlayResumeMusicIcon(ImageButton button) {
        if (isMusicPlaying())
            button.setImageResource(R.drawable.pause);
        else
            button.setImageResource(R.drawable.play);
    }

    /**
     * Shows the alarm time when an alarm has been set
     */
    private void showAlarmTime() {
        // runs on the main thread
        new android.os.Handler(context.getMainLooper()).post(() -> alarmCountdownText.setVisibility(View.VISIBLE));
    }

    /**
     * Hides the alarm time once an alarm has gone off
     *
     * @noinspection WeakerAccess
     */
    void hideAlarmtime() {
        // runs on the main thread
        new android.os.Handler(context.getMainLooper()).post(() -> alarmCountdownText.setVisibility(View.GONE));
    }

    // ------------------------------- HELPER FUNCTION -------------------------------

    /**
     * Check if music is currently being played
     *
     * @return true if music is playing, false otherwise
     */
    public boolean isMusicPlaying() {
        return null != mediaPlayer && mediaPlayer.isPlaying();
    }
}
