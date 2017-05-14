package me.dsbalaban.musicplayer;

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.content.ContentUris;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;

// Notification
import android.app.Notification;
import android.app.PendingIntent;
import android.widget.TextView;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener    {

    private MediaPlayer player;
    private ArrayList<Song> songs;
    private int songPos;
    private final IBinder musicBind = new MusicBinder();
    private Activity activity;

    private String songTitle = "";
    private static final int NOTIFY_ID = 1;

    private boolean shuffle = false;
    private Random rand;

    public void onCreate() {
        super.onCreate();

        songPos = 0;
        player = new MediaPlayer();
        rand = new Random();

        initMusicPlayer();
    }

    public void initMusicPlayer() {
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    public void setList(ArrayList<Song> songs) {
        this.songs = songs;
    }
    public void setActivity(Activity a) {
        this.activity = a;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (player.getCurrentPosition() > 0) {
            mp.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);

        Notification notification = builder.build();

        startForeground(NOTIFY_ID, notification);
    }

    public void setSong(int songIndex) {
        this.songPos = songIndex;
    }

    public void playSong() {
        player.reset();

        Song playSong = songs.get(songPos);
        songTitle = playSong.getTitle();

        long currentSong = playSong.getID();

        Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currentSong);

        try {
            player.setDataSource(getApplicationContext(), trackUri);

            TextView t = (TextView) activity.findViewById(R.id.song_details_title);
            t.setText(songTitle);
        } catch (Exception e) {
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

        player.prepareAsync();
    }

    public int getPosition() {
        return player.getCurrentPosition();
    }

    public int getDuration() {
        return player.getDuration();
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public void pausePlayer() {
        player.pause();
    }

    public void seek(int position) {
        player.seekTo(position);
    }

    public void start() {
        player.start();
    }

    public void playPrevious() {
        songPos--;

        if (songPos < 0) {
            songPos = songs.size() - 1;
        }

        playSong();
    }

    public void playNext() {
        if (shuffle) {
            int newSong = songPos;

            while (newSong == songPos) {
                newSong = rand.nextInt(songs.size());
            }

            songPos = newSong;
        } else {
            songPos++;

            if (songPos == songs.size()) {
                songPos = 0;
            }
        }

        playSong();
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }

    public void setShuffle() {
        shuffle = !shuffle;
    }
}
