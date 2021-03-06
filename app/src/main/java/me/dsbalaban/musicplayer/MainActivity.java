package me.dsbalaban.musicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.ListView;
import android.provider.MediaStore.Audio;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController;
import android.widget.ViewSwitcher;

import me.dsbalaban.musicplayer.MusicService.MusicBinder;

public class MainActivity extends Activity implements MediaController.MediaPlayerControl {
    private ArrayList<Song> songList;
    private ArrayList<Song> favoriteSongs;
    private ListView songView;
    private MusicService musicService;
    private Intent playIntent;
    private boolean musicBound = false;
    private DBHelper dbHelper = null;
    private SQLiteDatabase db = null;

    private MusicController musicController;
    private boolean paused = false;
    private boolean playbackPaused = false;

    private boolean showingFavorites = false;
    private boolean shuffling = false;

    private Menu menu = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = DBHelper.getInstance(this);
        db = dbHelper.getWritableDatabase();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

                // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                // app-defined int constant

                return;
            }
        }

        songView = (ListView)findViewById(R.id.song_list);
        songList = new ArrayList<>();

        getSongList();

        refreshSongListView(songList);

        setController();
    }

    private void refreshSongListView(ArrayList<Song> list) {
        Collections.sort(list, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        SongAdapter songAdapter = new SongAdapter(this, list);
        songView.setAdapter(songAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        this.menu = menu;

        return true;
    }

    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder) service;

            musicService = binder.getService();

            musicService.setList(songList);
            musicService.setActivity(MainActivity.this);

            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        ArrayList<Long> favoritesIds = dbHelper.getFavoritesIds();
        favoriteSongs = new ArrayList<>();

        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleColumn = musicCursor.getColumnIndex(Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(Audio.Media.ARTIST);

            do {
                long currentId = musicCursor.getLong(idColumn);
                String currentTitle = musicCursor.getString(titleColumn);
                String currentArtist = musicCursor.getString(artistColumn);

                Song songToAdd = new Song(currentId, currentTitle, currentArtist);

                if (favoritesIds.contains(currentId)) {
                    songToAdd.toggleFavorite();
                    favoriteSongs.add(songToAdd);
                }

                songList.add(songToAdd);
            } while (musicCursor.moveToNext());
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                this.shuffleAction(item);
                break;
            case R.id.action_end:
                ViewSwitcher viewSwitcher = (ViewSwitcher) findViewById(R.id.main_view_switcher);

                if (viewSwitcher.getDisplayedChild() == 0) {
                    this.exitAction();
                } else {
                    this.backAction(viewSwitcher, item);
                }

                break;
            case R.id.action_show_favorites:
                this.showFavorites(item);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void shuffleAction(MenuItem item) {
        shuffling = !shuffling;
        musicService.toggleShuffle();

        if (shuffling) {
            item.setIcon(getResources().getDrawable(R.drawable.rand_becca, this.getTheme()));
        } else {
            item.setIcon(getResources().getDrawable(R.drawable.rand, this.getTheme()));
        }
    }

    private void exitAction() {
        stopService(playIntent);
        musicService = null;
        System.exit(0);
    }

    private void backAction(ViewSwitcher viewSwitcher, MenuItem item) {
        viewSwitcher.setDisplayedChild(0);
        item.setIcon(getResources().getDrawable(R.drawable.end, this.getTheme()));
    }

    private void showFavorites(MenuItem item) {
        showingFavorites = !showingFavorites;
        item.setChecked(showingFavorites);

        if (showingFavorites) {
            refreshSongListView(favoriteSongs);
            musicService.setSongsList(favoriteSongs);
            item.setIcon(getResources().getDrawable(R.drawable.ic_favorite_border_becca_24dp, this.getTheme()));
        } else {
            refreshSongListView(songList);
            musicService.setSongsList(songList);
            item.setIcon(getResources().getDrawable(R.drawable.ic_favorite_border_black_24dp, this.getTheme()));
        }
    }

    @Override
    public void onDestroy() {
        stopService(playIntent);
        musicService = null;

        super.onDestroy();
    }

    private void setController() {
        musicController = new MusicController(this);

        musicController.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevious();
            }
        });

        musicController.setMediaPlayer(this);
        musicController.setAnchorView(findViewById(R.id.song_details_layout));
    }

    @Override
    public void start() {
        playbackPaused = false;
        musicService.start();
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicService.pausePlayer();
    }

    @Override
    public int getDuration() {
        if (musicService != null && musicBound) {
            return musicService.getDuration();
        }

        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (musicService != null && musicBound && musicService.isPlaying()) {
            return musicService.getPosition();
        }

        return 0;
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

    private void handlePlaybackPaused() {
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
    }

    public void playNext() {
        musicService.playNext();
        handlePlaybackPaused();
        musicController.show(0);
    }

    public void playPrevious() {
        musicService.playPrevious();
        handlePlaybackPaused();
        musicController.show(0);
    }

    @Override
    protected void onPause() {
        super.onPause();

        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (paused) {
            setController();
            paused = false;
        }
    }

    @Override
    protected void onStop() {
        musicController.hide();

        super.onStop();
    }

    // UI Interaction Methods

    public void songPicked(View view) {
        int songIndex = Integer.parseInt(view.getTag().toString());

        musicService.setSong(songIndex);
        handlePlaybackPaused();
        musicService.playSong();

        ViewSwitcher viewSwitcher = (ViewSwitcher) findViewById(R.id.main_view_switcher);
        viewSwitcher.showNext();

        this.menu.getItem(2).setIcon(getResources().getDrawable(R.drawable.ic_arrow_back_black_24dp, this.getTheme()));

        musicController.show(0);
    }

    public void toggleFavorite(View view) {
        int songIndex = musicService.getSongPos();

        Song song = songList.get(songIndex);

        if (song.isFavorite()) {
            String selection = FavoritesContract.FavoritesEntry.COLUMN_NAME_SONG_ID + " LIKE ?";
            String songId = String.valueOf(song.getID());
            String[] selectionArgs = { songId };

            db.delete(FavoritesContract.FavoritesEntry.TABLE_NAME, selection, selectionArgs);
            favoriteSongs.remove(song);
        } else {
            ContentValues values = new ContentValues();
            values.put(FavoritesContract.FavoritesEntry.COLUMN_NAME_SONG_ID, song.getID());

            db.insert(FavoritesContract.FavoritesEntry.TABLE_NAME, null, values);
            favoriteSongs.add(song);
        }
    }
}
