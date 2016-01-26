package com.example.fk.musicplayer.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.fk.musicplayer.MainActivity;
import com.example.fk.musicplayer.R;
import com.example.fk.musicplayer.model.Music;

import java.util.ArrayList;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener
//        , AudioManager.OnAudioFocusChangeListener
{

    private MediaPlayer player;
    private ArrayList<Music> musicList;
    private int musicPosition;
    private String musicTitle = "";

    private final IBinder musicBind = new MusicBinder();

    private static final int NOTIFY_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        musicPosition = 0;
        player = new MediaPlayer();

//        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
//                AudioManager.AUDIOFOCUS_GAIN);
//
//        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
//            Toast.makeText(getBaseContext(), "Could not play, another audio is in use", Toast.LENGTH_SHORT).show();
//        } else {
            initMusicPlayer();
//        }

    }

    private void initMusicPlayer() {
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }


    public void setList(ArrayList<Music> musics){
        musicList = musics;
    }
    public void setMusic(int index){
        musicPosition = index;
    }


    ///////////////////////
    // Binder
    ///////////////////////
    public class MusicBinder extends Binder {
        public MusicService getService() {
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void playMusic() {
        player.reset();

        Music playMusic = musicList.get(musicPosition);
        musicTitle = playMusic.getTitle();
        long currSong = playMusic.getId();

        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);

        try {
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e) {
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

        player.prepareAsync();
    }

    public int getPosn(){
      return player.getCurrentPosition();
    }

    public int getDur(){
      return player.getDuration();
    }

    public boolean isPlaying(){
      return player.isPlaying();
    }

    public void pausePlayer(){
      player.pause();
    }

    public void seek(int posn){
      player.seekTo(posn);
    }

    public void go(){
      player.start();
    }


    public void playPrev(){
      musicPosition--;
      if (musicPosition < 0) {
          musicPosition = musicList.size() - 1;
      }
      playMusic();
    }

    public void playNext(){
      musicPosition++;
      if (musicPosition >= musicList.size()) {
          musicPosition = 0;
      }
      playMusic();
    }

//    @Override
//    public void onAudioFocusChange(int focusChange) {
//        switch (focusChange) {
//            case AudioManager.AUDIOFOCUS_GAIN:
//                // resume playback
//                if (player == null) initMusicPlayer();
//                else if (!player.isPlaying()) player.start();
//                player.setVolume(1.0f, 1.0f);
//                break;
//
//            case AudioManager.AUDIOFOCUS_LOSS:
//                // Lost focus for an unbounded amount of time: stop playback and release media player
//                if (player.isPlaying()) player.stop();
//                player.release();
//                player = null;
//                break;
//
//            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
//                // Lost focus for a short time, but we have to stop
//                // playback. We don't release the media player because playback
//                // is likely to resume
//                if (player.isPlaying()) player.pause();
//                break;
//
//            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
//                // Lost focus for a short time, but it's ok to keep playing
//                // at an attenuated level
//                if (player.isPlaying()) player.setVolume(0.1f, 0.1f);
//                break;
//        }
//    }
//




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
        
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Notification.Builder builder = new Notification.Builder(this);
        builder.setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_play)
                .setTicker(musicTitle)
                .setOngoing(true)
                .setContentTitle(getString(R.string.Playing))
                .setContentText(musicTitle);

        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.build();
        }
        startForeground(NOTIFY_ID, notification);

        // Broadcast intent to activity to let it know the media player has been prepared
        Intent onPreparedIntent = new Intent("MEDIA_PLAYER_PREPARED");
        LocalBroadcastManager.getInstance(this).sendBroadcast(onPreparedIntent);
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }
}
