package com.example.fk.musicplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController;

import com.example.fk.musicplayer.adapter.MusicAdapter;
import com.example.fk.musicplayer.controller.MusicController;
import com.example.fk.musicplayer.model.Music;
import com.example.fk.musicplayer.service.MusicService;
import com.example.fk.musicplayer.service.MusicService.MusicBinder;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnItemClick;

/**
 * Refs:
 * http://developer.android.com/intl/pt-br/guide/topics/media/mediaplayer.html#audiofocus
 * http://code.tutsplus.com/tutorials/create-a-music-player-on-android-project-setup--mobile-22764
 * https://github.com/SueSmith/android-music-player
 * http://developer.android.com/intl/pt-br/guide/topics/media/mediaplayer.html
 * https://disqus.com/home/discussion/mobile-tuts/android_sdk_create_a_music_player_8211_app_setup_and_song_list/
 * https://jakewharton.github.io/butterknife/javadoc/butterknife/OnItemClick.html
 */

public class MainActivity extends AppCompatActivity implements MediaController.MediaPlayerControl {

    @Bind(R.id.lvMusic) ListView lvMusic;

    private ArrayList<Music> musicArrayList;
    private MusicService musicService;
    private Intent playIntent;
    private boolean musicBound = false;

    private MusicController musicController;

    private boolean paused = false;
    private boolean playbackPaused = false;
    private boolean isResuming = false;


    private MusicIntentReceiver myReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        musicArrayList = (ArrayList<Music>) getMusicList();
        lvMusic.setAdapter(new MusicAdapter(this, musicArrayList));

        setController();

        myReceiver = new MusicIntentReceiver();
    }

    @OnItemClick(R.id.lvMusic) void onMusicClick(int position) {
        musicService.setMusic(position);
        musicService.playMusic();

        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        // using broadcast
//        musicController.show(0);
    }

    private final ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
            musicService = binder.getService();

            musicService.setList(musicArrayList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    private List<Music> getMusicList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        ArrayList musicList = new ArrayList<>();

        if(musicCursor!=null && musicCursor.moveToFirst()){

            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);

            do {
                long id = musicCursor.getLong(idColumn);
                String title = musicCursor.getString(titleColumn);
                String artist = musicCursor.getString(artistColumn);
                musicList.add(new Music(id, title, artist));
            }
            while (musicCursor.moveToNext());
            musicCursor.close();
        }
        return musicList;
    }

    @Override
    protected void onPause(){
        super.onPause();
        paused = true;
        unregisterReceiver(myReceiver);
    }

    @Override
    protected void onResume() {
        Log.d("asdf", "onResume");
        isResuming = true;

        if (paused) {
            setController();
            paused = false;
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(myReceiver, filter);

        // Set up receiver for media player onPrepared broadcast
        LocalBroadcastManager.getInstance(this).registerReceiver(onPrepareReceiver,
                new IntentFilter("MEDIA_PLAYER_PREPARED"));

        super.onResume();
    }

    @Override
    protected void onStop() {
        musicController.hide();
        super.onStop();
    }

    @Override
	protected void onDestroy() {
		stopService(playIntent);
		musicService = null;
		super.onDestroy();
	}

    // Broadcast receiver to determine when music player has been prepared
    private BroadcastReceiver onPrepareReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            // When music player has been prepared, show controller
            musicController.show(0);
        }
    };

    private void setController() {
        if (musicController == null) {
            musicController = new MusicController(this);
        }

		musicController.setPrevNextListeners(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				playNextMusic();
			}
		}, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				playPrevMusic();
			}
		});

        musicController.setMediaPlayer(this);
        musicController.setAnchorView(lvMusic);
        musicController.setEnabled(true);
    }

	private void playNextMusic(){
		musicService.playNext();
		if (playbackPaused) {
			setController();
			playbackPaused = false;
		}
		musicController.show(0);
	}

	private void playPrevMusic(){
		musicService.playPrev();
		if (playbackPaused) {
			setController();
			playbackPaused = false;
		}
        musicController.show(0);
	}


    ///////////////////////
    // MediaPlayerControl methods
    ///////////////////////

    @Override
    public void start() {
		musicService.go();

    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicService.pausePlayer();
    }

    @Override
    public int getDuration() {
        if(musicService != null && musicBound && musicService.isPlaying())
            return musicService.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (musicService != null && musicBound && musicService.isPlaying()) {
            return musicService.getPosn();
        } else {
            return 0;
        }
    }

    @Override
    public void seekTo(int pos) {
		musicService.seek(pos);

    }

    @Override
    public boolean isPlaying() {
        return musicService != null && musicBound && musicService.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    private class MusicIntentReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        Log.d("MainActivity", "Headset is unplugged");
                        if (isResuming) {
                            Log.d("MainActivity", "is resuming");
                        }
                        if (isPlaying() && !isResuming) {
                            pause();
                            Log.d("MainActivity", "action headset pausing");
                        }
                        isResuming = false;
                        break;
                    case 1:
                        Log.d("MainActivity", "Headset is plugged");
                        break;
                    default:
                        Log.d("MainActivity", "I have no idea what the headset state is");
                }
            }
        }
    }

}
