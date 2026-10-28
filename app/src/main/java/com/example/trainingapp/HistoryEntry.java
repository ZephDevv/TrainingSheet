package com.example.trainingapp;

public class HistoryEntry {
    public final long id;
    public final String exerciseId;
    public final String exerciseName;
    public final long dateMs;
    public final int sets;
    public final int reps;
    public final double weight;

    public HistoryEntry(long id, String exerciseId, String exerciseName,
                        long dateMs, int sets, int reps, double weight) {
        this.id = id;
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.dateMs = dateMs;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
    }

    /** Training volume: sets x reps x weight. */
    public double volume() {
        return (double) sets * reps * weight;
    }

    /** Estimated one-rep max (Epley formula). */
    public double estimatedOneRepMax() {
        return weight * (1.0 + reps / 30.0);
    }
}