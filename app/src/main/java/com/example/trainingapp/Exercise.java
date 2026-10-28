package com.example.trainingapp;

import java.util.UUID;

public class Exercise {
    public String id = UUID.randomUUID().toString();
    public String name = "";
    public String description = "";
    public int sets = 0;
    public int reps = 0;
    public double weight = 0;
}