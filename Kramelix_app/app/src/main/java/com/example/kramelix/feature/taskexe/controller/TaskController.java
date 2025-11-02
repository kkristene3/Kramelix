package com.example.kramelix.feature.taskexe.controller;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaMetadataRetriever;
import android.os.SystemClock;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


import com.example.kramelix.R;

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
 *     <li>Own a lightweight registry of supported tasks (e.g. music playback, alarms).</li>
 *     <li>Parse task command strings and dispatch to the correct implementation.</li>
 *     <li>Manage system resources such as {@link android.media.MediaPlayer} safely.</li>
 *     <li>Serve as the feature-level extension point for future “assistant-like” abilities.</li>
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
 * @author Alex Oprea, Kristen Duong
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

    String songName;

    // -------------------- NOTIFICATION CHANNEL --------------------
    private static final String CHANNEL_ID = "Media Channel";
    NotificationManager notificationManager;

    // ----------------------------- UI -----------------------------
    private LinearLayout musicControlsLayout;
    private ImageButton playResumeMusicButton;

    // ------------------------- LIFECYCLE -------------------------

    /**
     * Constructor
     *
     * @param context Any valid {@link Context}; the application context will be retained.
     */
    private TaskController(@NonNull Context context) {
        this.context = context.getApplicationContext();
        initMediaSession();
        createNotificationChannel();
    }

    /**
     * The public "gateway" to access the TaskController object
     * Creates a new object on the first call, afterwards, the pre-existing instance is returned
     *
     * @param context Any valid {@link Context}; the application context will be retained.
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

        // PROCESS: create controller
        MediaControllerCompat mediaController = new MediaControllerCompat(context, mediaSession.getSessionToken());
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
        else if (task.contains("playMusicArtist(")){
            String[] taskParams = getParams(task);
            if (taskParams.length == 1){
                return playMusicArtist(taskParams[0]);
            }
        }

        //Task: Play Music based on a Genre
        else if (task.contains("playMusicGenre(")){
            String[] taskParams=  getParams(task);
            if (taskParams.length == 1){
                return playMusicGenre(taskParams[0]);
            }
        }

        // Task: Set an Alarm
        else if (task.contains("setAlarm(")) {

            // extract the parameters from the string
            String[] taskParams = getParams(task);

            // TODO: currently only accepts time in minutes,
            //  a user may want to set a timer using hours, or at an exact time; will have to adjust
            if (1 == taskParams.length) {
                return setAlarm(taskParams[0]);
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
        return playMusic(song);
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
            songName = song.trim().toLowerCase().replace(" ", "_");

            // PROCESS: get resource ID (music file)
            @SuppressLint("DiscouragedApi") int resId = context.getResources().getIdentifier(songName, "raw", context.getPackageName());

            // PROCESS: if cannot find song in folder
            if (0 == resId) {
                System.out.println("TaskController - Song not found in res/raw: " + songName);
                return false;
            }

            // PROCESS: create and start music player
            mediaPlayer = MediaPlayer.create(context, resId);
            mediaPlayer.setOnCompletionListener(mp -> stopMusic()); // TODO
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
    private boolean playMusicArtist(String artist){
        String song = getMusicBasedOnMetadata("Artist", artist);
        if (song != null){
            playMusic(song);
        }
        //TODO error handle the songs we can't find =(
        else{
            System.out.println("couldn't find song =(");
        }
        return true;
    }

    /**
     * Function for playing a song based on a given genre
     *
     * @param genre The genre of the song to play
     * @return Boolean representing the success of the music playing. Success = true, any error (including unable to find song) is false
     */
    private boolean playMusicGenre(String genre){
        String song = getMusicBasedOnMetadata("Genre", genre);
        if (song != null){
            playMusic(song);
        }
        //TODO error handle the songs we can't find =(
        else{
            System.out.println("couldn't find song =(");
        }
        return true;
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
        if (mediaPlayer != null) {
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
            //noinspection DynamicRegexReplaceableByCompiledPattern
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
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
    public void showNotification() {

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
                .setContentTitle("Now Playing...")
                .setContentText(songName)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0)) // 0 = play/pause
                .addAction(new NotificationCompat.Action(
                        R.drawable.play, "Play/Pause", playPausePendingIntent // index 0
                ));

        // PROCESS: get the system's notification center and post the notification
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    // ------------------------------- HANDLE UI CHANGES -------------------------------

    /**
     * Associate the music controls layout with a Linear Layout contained
     *
     * @param layout - the Linear Layout containing the music control buttons;
     *               received from MainActivity.java
     */
    public void setMusicControlsLayout (LinearLayout layout) {
        this.musicControlsLayout = layout;
    }

    /**
     * Set the layout to VISIBLE
     */
    private void showMusicControls() {
        if (musicControlsLayout != null) {
            // runs on the main thread
            new android.os.Handler(context.getMainLooper()).post(() -> {
                musicControlsLayout.setVisibility(View.VISIBLE);
            });
        }
    }

    /**
     * Set the layout to INVISIBLE
     */
    private void hideMusicControls() {
        if (musicControlsLayout != null) {
            // runs on the main thread
            new android.os.Handler(context.getMainLooper()).post(() -> {
                musicControlsLayout.setVisibility(View.INVISIBLE);
            });
        }
    }

    /**
     * Associate the Play/Resume music button with an ImageButton
     * @param button - the Image Button used to control playing and resuming music
     */
    public void setPlayResumeMusicButton (ImageButton button) {
        this.playResumeMusicButton = button;
    }

    /**
     * Change the play/resume button icon based on whether music is being played or not
     *
     * @param button - the Image Button used to control playing and resuming music
     */
    private void switchPlayResumeMusicIcon (ImageButton button) {
        if (isMusicPlaying())
            button.setImageResource(R.drawable.pause);
        else
            button.setImageResource(R.drawable.play);
    }
    /**
     * Get the song title, artist or genre from the music file's metadata
     *
     * @param type - The type of metadata expected as a returned value
     * @param data - The data that we want to choose a song based off
     *
     * @return string containing desired metadata info
     * */
    private String getMusicBasedOnMetadata(String type, String data){
        //list of matching songs
        List<String> songList = new ArrayList<>();

        List<Integer> musicIds = getAllSongResourceIds(context);

        for (int id:musicIds){
            //create a new Metadata retriever
            MediaMetadataRetriever meta = new MediaMetadataRetriever();
            AssetFileDescriptor afd;
            String songTitle = null;

            try{
                afd = context.getResources().openRawResourceFd(id);
                songTitle = context.getResources().getResourceEntryName(id);
                if (afd == null){
                    continue;
                }
                //set the metadata object to the current file that we want to examine
                meta.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());

                //look for any artists in the f
                if (type.equals("Artist")){
                    String artist = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

                    if (artist != null && artist.toLowerCase().contains(data.toLowerCase())){
                        if (songTitle!=null) {
                            songList.add(songTitle);
                        }
                    }
                }
                if (type.equals("Genre")){
                    String genre = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);

                    if (genre != null && genre.toLowerCase().contains(data.toLowerCase())){
                        if (songTitle!=null) {
                            songList.add(songTitle);
                        }
                    }
                }
                //close the created resources
                try {
                    meta.release();
                    afd.close();
                }
                catch (Exception ignored) {}
            }
            catch (Exception e){
                System.out.println("TaskController - Runtime, failed to find music file: " + e);
            }
        }
        //turn the song list into an array for easier indexing
        String[] iterableSongList = songList.toArray(new String[0]);

        //if there is only one song that fits the requirements, return that song
        if (iterableSongList.length == 1){
            return iterableSongList[0];
        }
        //if there are no songs that fit the requirement, return null
        else if (iterableSongList.length == 0){
            return null;
        }
        //if there are multiple songs that fit the requirement, we must choose a random song to return
        else{
            int randomNum = (int)(Math.random() * iterableSongList.length);
            return iterableSongList[randomNum];
        }
    }

    /**
     * Get the song title, artist or genre from the music file's metadata
     *
     * @param context - The current context of the app
     *
     * @return List<Integer> containing the list of music file ids
     * */
    private static List<Integer> getAllSongResourceIds(Context context){
        List<Integer> ids = new ArrayList<>();
        try{
            Class<?> raw = Class.forName(context.getPackageName() + ".R$raw");
            Field[] fields = raw.getDeclaredFields();
            for (int i = 0; i<fields.length; i++){
                ids.add(fields[i].getInt(null));
            }
        }
        catch(Exception e){
            System.out.println(e);
        }
        return ids;
    }

    /**
     * Check if music is currently being played
     *
     * @return true if music is playing, false otherwise
     */
    public boolean isMusicPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

}
