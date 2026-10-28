package com.example.trainingapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class HistoryDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "training_history.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "history";

    private static HistoryDbHelper instance;

    public static synchronized HistoryDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new HistoryDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private HistoryDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "exercise_id TEXT NOT NULL, "
                + "exercise_name TEXT NOT NULL, "
                + "date_ms INTEGER NOT NULL, "
                + "sets INTEGER NOT NULL, "
                + "reps INTEGER NOT NULL, "
                + "weight REAL NOT NULL)");
        db.execSQL("CREATE INDEX idx_history_exercise ON " + TABLE + " (exercise_id, date_ms)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Future schema migrations go here.
    }

    public long insert(HistoryEntry e) {
        ContentValues v = new ContentValues();
        v.put("exercise_id", e.exerciseId);
        v.put("exercise_name", e.exerciseName);
        v.put("date_ms", e.dateMs);
        v.put("sets", e.sets);
        v.put("reps", e.reps);
        v.put("weight", e.weight);
        return getWritableDatabase().insert(TABLE, null, v);
    }

    /** All entries of one exercise, oldest first. */
    public List<HistoryEntry> getForExercise(String exerciseId) {
        List<HistoryEntry> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE, null,
                "exercise_id = ?", new String[]{exerciseId},
                null, null, "date_ms ASC, _id ASC")) {
            while (c.moveToNext()) {
                list.add(new HistoryEntry(
                        c.getLong(c.getColumnIndexOrThrow("_id")),
                        c.getString(c.getColumnIndexOrThrow("exercise_id")),
                        c.getString(c.getColumnIndexOrThrow("exercise_name")),
                        c.getLong(c.getColumnIndexOrThrow("date_ms")),
                        c.getInt(c.getColumnIndexOrThrow("sets")),
                        c.getInt(c.getColumnIndexOrThrow("reps")),
                        c.getDouble(c.getColumnIndexOrThrow("weight"))));
            }
        }
        return list;
    }

    /** Distinct exercises that have history: {exerciseId, latestName}, most recently changed first. */
    public List<String[]> getHistoryExercises() {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT exercise_id, exercise_name, MAX(date_ms) AS last_ms FROM "
                + TABLE + " GROUP BY exercise_id ORDER BY last_ms DESC";
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            while (c.moveToNext()) {
                list.add(new String[]{c.getString(0), c.getString(1)});
            }
        }
        return list;
    }

    public void deleteEntry(long id) {
        getWritableDatabase().delete(TABLE, "_id = ?", new String[]{String.valueOf(id)});
    }
}