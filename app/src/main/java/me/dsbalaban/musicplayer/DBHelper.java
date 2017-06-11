package me.dsbalaban.musicplayer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

import java.util.ArrayList;

import me.dsbalaban.musicplayer.FavoritesContract.FavoritesEntry;

public class DBHelper extends SQLiteOpenHelper{
    private static final String DB_NAME = "MusicPlayer.db";

    private static final String SQL_CREATE_ENTRIES = "" +
            "CREATE TABLE " + FavoritesEntry.TABLE_NAME + "(" +
            FavoritesEntry._ID + " INTEGER PRIMARY KEY," +
            FavoritesEntry.COLUMN_NAME_SONG_ID + " INTEGER, " +
            "UNIQUE (" + FavoritesEntry.COLUMN_NAME_SONG_ID + ") " +
            "ON CONFLICT REPLACE)";

    private static DBHelper instance = null;

    private DBHelper(Context c) {
        super(c, DB_NAME, null, 1);
    }

    public static DBHelper getInstance(Context c) {
        if (instance == null) {
            instance = new DBHelper(c);
        }

        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void addFavorite(long id) {
        SQLiteDatabase writeableDB = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(FavoritesContract.FavoritesEntry.COLUMN_NAME_SONG_ID, id);

        writeableDB.insert(FavoritesContract.FavoritesEntry.TABLE_NAME, null, values);
    }

    public ArrayList<Long> getFavoritesIds() {
        SQLiteDatabase readableDB = this.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String[] projection = {
                FavoritesEntry.COLUMN_NAME_SONG_ID
        };

        String sortOrder = FavoritesEntry.COLUMN_NAME_SONG_ID + " DESC";

        qb.setTables(FavoritesEntry.TABLE_NAME);
        qb.setDistinct(true);

        Cursor cursor = qb.query(
            readableDB,
            projection,
            null,
            null,
            null,
            null,
            sortOrder
        );

        ArrayList<Long> songIds = new ArrayList<>();

        while (cursor.moveToNext()) {
            long songId = cursor.getInt(
                    cursor.getColumnIndex(FavoritesEntry.COLUMN_NAME_SONG_ID)
            );

            songIds.add(songId);
        }

        cursor.close();

        return songIds;
    }
}
