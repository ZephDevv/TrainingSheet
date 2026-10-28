package com.example.trainingapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class AnalysisActivity extends AppCompatActivity {

    private enum Metric {
        WEIGHT("Weight", "kg"),
        REPS("Reps", "reps"),
        SETS("Sets", "sets"),
        VOLUME("Volume", "kg"),
        ONE_RM("Est. 1RM", "kg");

        final String label;
        final String unit;

        Metric(String label, String unit) {
            this.label = label;
            this.unit = unit;
        }

        double valueOf(HistoryEntry e) {
            switch (this) {
                case WEIGHT:
                    return e.weight;
                case REPS:
                    return e.reps;
                case SETS:
                    return e.sets;
                case VOLUME:
                    return e.volume();
                default:
                    return e.estimatedOneRepMax();
            }
        }
    }

    private static class Option {
        final String exerciseId;
        final String label;
        final boolean deleted;

        Option(String exerciseId, String label, boolean deleted) {
            this.exerciseId = exerciseId;
            this.label = label;
            this.deleted = deleted;
        }
    }

    private final SimpleDateFormat dayFmt = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
    private final SimpleDateFormat dateTimeFmt = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US);

    private HistoryDbHelper db;
    private final List<Option> options = new ArrayList<>();
    private Option current;
    private Metric metric = Metric.WEIGHT;
    private List<HistoryEntry> entries = new ArrayList<>();

    private View svAnalysis, llNoExercises, cardSummary;
    private TextView tvExercisePicker, tvChartTitle, tvChartEmpty;
    private TextView tvLatestValues, tvFirst, tvLatest, tvChange, tvBest, tvEntries;
    private TextView tvHistoryTitle, tvHistoryHint;
    private LinearLayout llMetricChips, llHistory;
    private LineChartView chart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_analysis);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.analysisRoot), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        bindViews();
        db = HistoryDbHelper.getInstance(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvExercisePicker.setOnClickListener(v -> showPicker());

        loadOptionsAndSelect();
    }

    private void bindViews() {
        svAnalysis = findViewById(R.id.svAnalysis);
        llNoExercises = findViewById(R.id.llNoExercises);
        cardSummary = findViewById(R.id.cardSummary);
        tvExercisePicker = findViewById(R.id.tvExercisePicker);
        tvChartTitle = findViewById(R.id.tvChartTitle);
        tvChartEmpty = findViewById(R.id.tvChartEmpty);
        tvLatestValues = findViewById(R.id.tvLatestValues);
        tvFirst = findViewById(R.id.tvFirst);
        tvLatest = findViewById(R.id.tvLatest);
        tvChange = findViewById(R.id.tvChange);
        tvBest = findViewById(R.id.tvBest);
        tvEntries = findViewById(R.id.tvEntries);
        tvHistoryTitle = findViewById(R.id.tvHistoryTitle);
        tvHistoryHint = findViewById(R.id.tvHistoryHint);
        llMetricChips = findViewById(R.id.llMetricChips);
        llHistory = findViewById(R.id.llHistory);
        chart = findViewById(R.id.chart);
    }

    // ---------- Exercise selection ----------
    private void buildOptions() {
        options.clear();
        Set<String> seen = new HashSet<>();

        TrainingRepository.TrainingData data = TrainingRepository.load(this);
        for (Day d : data.days) {
            for (Exercise ex : d.exercises) {
                options.add(new Option(ex.id, d.name + " - " + ex.name, false));
                seen.add(ex.id);
            }
        }
        // Exercises that were deleted but still have history
        for (String[] h : db.getHistoryExercises()) {
            if (!seen.contains(h[0])) {
                options.add(new Option(h[0], h[1] + " (deleted)", true));
            }
        }
    }

    private Option defaultOption() {
        List<String[]> hist = db.getHistoryExercises();
        if (!hist.isEmpty()) {
            for (Option o : options) {
                if (o.exerciseId.equals(hist.get(0)[0])) return o;
            }
        }
        return options.get(0);
    }

    private void loadOptionsAndSelect() {
        buildOptions();
        if (options.isEmpty()) {
            svAnalysis.setVisibility(View.GONE);
            llNoExercises.setVisibility(View.VISIBLE);
            return;
        }
        svAnalysis.setVisibility(View.VISIBLE);
        llNoExercises.setVisibility(View.GONE);
        selectOption(defaultOption());
    }

    private void showPicker() {
        if (options.isEmpty()) return;
        String[] labels = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            labels[i] = options.get(i).label;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select exercise")
                .setItems(labels, (d, which) -> selectOption(options.get(which)))
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
        }
    }

    private void selectOption(Option option) {
        current = option;
        tvExercisePicker.setText(option.label + "  \u25BE");
        entries = db.getForExercise(option.exerciseId);

        // Bodyweight exercises (no weight) start with the reps chart
        metric = Metric.WEIGHT;
        if (!entries.isEmpty() && entries.get(entries.size() - 1).weight <= 0) {
            metric = Metric.REPS;
        }
        renderChips();
        render();
    }

    // ---------- Rendering ----------
    private void renderChips() {
        llMetricChips.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (final Metric m : Metric.values()) {
            TextView chip = (TextView) inflater.inflate(R.layout.item_day_chip, llMetricChips, false);
            chip.setText(m.label);
            if (m == metric) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected);
                chip.setTextColor(Color.WHITE);
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip);
                chip.setTextColor(getColor(R.color.ts_primary));
            }
            chip.setOnClickListener(v -> {
                metric = m;
                renderChips();
                render();
            });
            llMetricChips.addView(chip);
        }
    }

    private long dayKey(long ms) {
        return (ms + TimeZone.getDefault().getOffset(ms)) / 86400000L;
    }

    private String weightText(double w) {
        return w > 0 ? LineChartView.formatNumber(w) + " kg" : "- kg";
    }

    private void render() {
        tvChartTitle.setText(metric.label + " over time (" + metric.unit + ")");

        // Every saved edit is one point on the chart
        List<HistoryEntry> valid = new ArrayList<>();
        for (HistoryEntry e : entries) {
            if (metric.valueOf(e) > 0) valid.add(e);
        }

        if (valid.isEmpty()) {
            chart.setVisibility(View.GONE);
            tvChartEmpty.setVisibility(View.VISIBLE);
            if (entries.isEmpty()) {
                tvChartEmpty.setText("No history yet. Change the sets, reps or weight of this "
                        + "exercise in the training sheet to start tracking your progress.");
            } else {
                tvChartEmpty.setText("No " + metric.label.toLowerCase(Locale.US)
                        + " values recorded for this exercise.");
            }
        } else {
            tvChartEmpty.setVisibility(View.GONE);
            chart.setVisibility(View.VISIBLE);
            long[] t = new long[valid.size()];
            double[] v = new double[valid.size()];
            for (int i = 0; i < valid.size(); i++) {
                t[i] = valid.get(i).dateMs;
                v[i] = metric.valueOf(valid.get(i));
            }
            chart.setData(t, v, metric.unit);
        }

        renderSummary(valid);
        renderHistory();
    }


    private void renderSummary(List<HistoryEntry> valid) {
        if (entries.isEmpty()) {
            cardSummary.setVisibility(View.GONE);
            return;
        }
        cardSummary.setVisibility(View.VISIBLE);

        HistoryEntry last = entries.get(entries.size() - 1);
        tvLatestValues.setText("Latest: " + last.sets + " sets \u00D7 " + last.reps
                + " reps \u00D7 " + weightText(last.weight));
        tvEntries.setText("Entries: " + entries.size());

        int vis = valid.isEmpty() ? View.GONE : View.VISIBLE;
        tvFirst.setVisibility(vis);
        tvLatest.setVisibility(vis);
        tvChange.setVisibility(vis);
        tvBest.setVisibility(vis);
        if (valid.isEmpty()) return;

        HistoryEntry first = valid.get(0);
        HistoryEntry latest = valid.get(valid.size() - 1);
        double fv = metric.valueOf(first);
        double lv = metric.valueOf(latest);
        double best = fv;
        for (HistoryEntry e : valid) {
            best = Math.max(best, metric.valueOf(e));
        }

        double change = lv - fv;
        String sign = change > 0 ? "+" : "";
        String changeText = sign + LineChartView.formatNumber(change) + " " + metric.unit;
        if (fv > 0) {
            changeText += String.format(Locale.US, " (%s%.1f%%)", sign, change / fv * 100.0);
        }

        tvFirst.setText("First: " + LineChartView.formatNumber(fv) + " " + metric.unit
                + " (" + dateTimeFmt.format(new Date(first.dateMs)) + ")");
        tvLatest.setText("Newest: " + LineChartView.formatNumber(lv) + " " + metric.unit
                + " (" + dateTimeFmt.format(new Date(latest.dateMs)) + ")");
        tvChange.setText("Change: " + changeText);
        tvBest.setText("Best: " + LineChartView.formatNumber(best) + " " + metric.unit);
    }

    private void renderHistory() {
        llHistory.removeAllViews();
        int vis = entries.isEmpty() ? View.GONE : View.VISIBLE;
        tvHistoryTitle.setVisibility(vis);
        tvHistoryHint.setVisibility(vis);
        if (entries.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        // Newest first
        for (int i = entries.size() - 1; i >= 0; i--) {
            final HistoryEntry e = entries.get(i);
            View row = inflater.inflate(R.layout.item_history, llHistory, false);
            ((TextView) row.findViewById(R.id.tvHistoryDate))
                    .setText(dateTimeFmt.format(new Date(e.dateMs)));
            ((TextView) row.findViewById(R.id.tvHistoryValues))
                    .setText(e.sets + " \u00D7 " + e.reps + " \u00D7 " + weightText(e.weight));
            row.setOnLongClickListener(v -> {
                confirmDelete(e);
                return true;
            });
            llHistory.addView(row);
        }
    }

    private void confirmDelete(final HistoryEntry e) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete entry")
                .setMessage("Delete this history entry from "
                        + dateTimeFmt.format(new Date(e.dateMs)) + "?")
                .setPositiveButton("Delete", (d, which) -> {
                    db.deleteEntry(e.id);
                    entries = db.getForExercise(current.exerciseId);
                    if (entries.isEmpty() && current.deleted) {
                        loadOptionsAndSelect();
                    } else {
                        render();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
        }
    }
}