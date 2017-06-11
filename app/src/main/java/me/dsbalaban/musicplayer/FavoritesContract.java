package me.dsbalaban.musicplayer;

import android.provider.BaseColumns;

public final class FavoritesContract {
    // prevent instantiation
    private FavoritesContract() {}

    public static class FavoritesEntry implements BaseColumns {
        public static final String TABLE_NAME = "favorites";
        public static final String COLUMN_NAME_SONG_ID = "song_id";
    }
}
