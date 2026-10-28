package com.example.trainingapp;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Loads and saves training days (JSON) and writes history entries (SQLite). */
public final class TrainingRepository {

    private static final String PREFS_NAME = "training_prefs";
    private static final String KEY_DATA = "data";

    private TrainingRepository() {
    }

    public static class TrainingData {
        public final List<Day> days = new ArrayList<>();
        public int selectedDay = -1;
    }

    public static TrainingData load(Context context) {
        TrainingData data = new TrainingData();
        String json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_DATA, null);
        if (json == null) return data;

        List<Exercise> migrated = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(json);
            JSONArray daysArr = root.optJSONArray("days");
            if (daysArr != null) {
                for (int i = 0; i < daysArr.length(); i++) {
                    JSONObject dayObj = daysArr.getJSONObject(i);
                    Day day = new Day();
                    day.name = dayObj.optString("name", "Day " + (i + 1));
                    JSONArray exArr = dayObj.optJSONArray("exercises");
                    if (exArr != null) {
                        for (int j = 0; j < exArr.length(); j++) {
                            JSONObject exObj = exArr.getJSONObject(j);
                            Exercise ex = new Exercise();
                            String id = exObj.optString("id", "");
                            if (id.isEmpty()) {
                                migrated.add(ex); // old data: keeps the freshly generated id
                            } else {
                                ex.id = id;
                            }
                            ex.name = exObj.optString("name", "");
                            ex.description = exObj.optString("description", "");
                            ex.sets = exObj.optInt("sets", 0);
                            ex.reps = exObj.optInt("reps", 0);
                            ex.weight = exObj.optDouble("weight", 0);
                            day.exercises.add(ex);
                        }
                    }
                    data.days.add(day);
                }
            }
            data.selectedDay = root.optInt("selected", -1);
            if (data.days.isEmpty()) {
                data.selectedDay = -1;
            } else if (data.selectedDay < 0 || data.selectedDay >= data.days.size()) {
                data.selectedDay = 0;
            }
        } catch (JSONException err) {
            err.printStackTrace();
            data.days.clear();
            data.selectedDay = -1;
            return data;
        }

        // Migration: exercises saved before history existed get an id and a first history entry
        if (!migrated.isEmpty()) {
            for (Exercise ex : migrated) {
                logExercise(context, ex);
            }
            save(context, data.days, data.selectedDay);
        }
        return data;
    }

    public static void save(Context context, List<Day> days, int selectedDay) {
        try {
            JSONArray daysArr = new JSONArray();
            for (Day d : days) {
                JSONObject dayObj = new JSONObject();
                dayObj.put("name", d.name);
                JSONArray exArr = new JSONArray();
                for (Exercise ex : d.exercises) {
                    JSONObject exObj = new JSONObject();
                    exObj.put("id", ex.id);
                    exObj.put("name", ex.name);
                    exObj.put("description", ex.description);
                    exObj.put("sets", ex.sets);
                    exObj.put("reps", ex.reps);
                    exObj.put("weight", ex.weight);
                    exArr.put(exObj);
                }
                dayObj.put("exercises", exArr);
                daysArr.put(dayObj);
            }
            JSONObject root = new JSONObject();
            root.put("selected", selectedDay);
            root.put("days", daysArr);

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_DATA, root.toString())
                    .apply();
        } catch (JSONException err) {
            err.printStackTrace();
        }
    }

    /** Writes the exercise's current sets/reps/weight to the history (skips all-empty values). */
    public static void logExercise(Context context, Exercise ex) {
        if (ex.sets <= 0 && ex.reps <= 0 && ex.weight <= 0) return;
        HistoryEntry entry = new HistoryEntry(0, ex.id, ex.name,
                System.currentTimeMillis(), ex.sets, ex.reps, ex.weight);
        HistoryDbHelper.getInstance(context).insert(entry);
    }
}