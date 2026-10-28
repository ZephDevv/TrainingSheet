package com.example.trainingapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private interface TextCallback {
        void onText(String text);
    }

    private final List<Day> days = new ArrayList<>();
    private int selectedDay = -1;   // index of the selected day, -1 = none
    private int editingIndex = -1;  // index of the exercise being edited, -1 = adding new

    // ---------- Views ----------
    private DrawerLayout drawerLayout;
    private LinearLayout drawerPanel;
    private View btnMenu;
    private TextView menuBmi, menuAnalysis;
    private LinearLayout llDaysContainer;
    private TextView tvAddDay;
    private LinearLayout llDayHeader;
    private TextView tvDayName, tvExerciseCount, tvRenameDay, tvDeleteDay;
    private LinearLayout llEmptyNoDays;
    private TextView btnCreateFirstDay;
    private ScrollView svContent;
    private LinearLayout cardAddExercise;
    private TextView tvFormTitle, btnCancelExercise, btnSaveExercise;
    private EditText etExerciseName, etExerciseDescription, etSets, etReps, etWeight;
    private LinearLayout llEmptyNoExercises, llExercisesContainer;
    private TextView fabAddExercise;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // The app is designed in black and white, so always use the light theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bindViews();

        // Keep content and the side menu clear of system bars (and the keyboard for content)
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            findViewById(R.id.main).setPadding(bars.left, bars.top, bars.right, bars.bottom);

            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            drawerPanel.setPadding(sys.left, sys.top, 0, sys.bottom);
            return insets;
        });

        loadData();
        setupListeners();
        refreshAll();
    }

    private void bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        drawerPanel = findViewById(R.id.drawerPanel);
        btnMenu = findViewById(R.id.btnMenu);
        menuBmi = findViewById(R.id.menuBmi);
        menuAnalysis = findViewById(R.id.menuAnalysis);
        llDaysContainer = findViewById(R.id.llDaysContainer);
        tvAddDay = findViewById(R.id.tvAddDay);
        llDayHeader = findViewById(R.id.llDayHeader);
        tvDayName = findViewById(R.id.tvDayName);
        tvExerciseCount = findViewById(R.id.tvExerciseCount);
        tvRenameDay = findViewById(R.id.tvRenameDay);
        tvDeleteDay = findViewById(R.id.tvDeleteDay);
        llEmptyNoDays = findViewById(R.id.llEmptyNoDays);
        btnCreateFirstDay = findViewById(R.id.btnCreateFirstDay);
        svContent = findViewById(R.id.svContent);
        cardAddExercise = findViewById(R.id.cardAddExercise);
        tvFormTitle = findViewById(R.id.tvFormTitle);
        etExerciseName = findViewById(R.id.etExerciseName);
        etExerciseDescription = findViewById(R.id.etExerciseDescription);
        etSets = findViewById(R.id.etSets);
        etReps = findViewById(R.id.etReps);
        etWeight = findViewById(R.id.etWeight);
        btnCancelExercise = findViewById(R.id.btnCancelExercise);
        btnSaveExercise = findViewById(R.id.btnSaveExercise);
        llEmptyNoExercises = findViewById(R.id.llEmptyNoExercises);
        llExercisesContainer = findViewById(R.id.llExercisesContainer);
        fabAddExercise = findViewById(R.id.fabAddExercise);
    }

    private void setupListeners() {
        // Side menu
        btnMenu.setOnClickListener(v -> {
            hideKeyboard();
            drawerLayout.openDrawer(GravityCompat.START);
        });
        menuBmi.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START, false);
            startActivity(new Intent(MainActivity.this, BmiActivity.class));
        });
        menuAnalysis.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START, false);
            startActivity(new Intent(MainActivity.this, AnalysisActivity.class));
        });

        // Training sheet
        tvAddDay.setOnClickListener(v -> promptAddDay());
        btnCreateFirstDay.setOnClickListener(v -> promptAddDay());
        tvRenameDay.setOnClickListener(v -> promptRenameDay());
        tvDeleteDay.setOnClickListener(v -> confirmDeleteDay());
        fabAddExercise.setOnClickListener(v -> showForm(-1));
        btnCancelExercise.setOnClickListener(v -> hideForm());
        btnSaveExercise.setOnClickListener(v -> saveExercise());

        // Back button: close the menu first, then the form, then leave the app
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (cardAddExercise.getVisibility() == View.VISIBLE) {
                    hideForm();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // ---------- Helpers ----------
    private Day currentDay() {
        if (selectedDay >= 0 && selectedDay < days.size()) {
            return days.get(selectedDay);
        }
        return null;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private String formatWeight(double w) {
        if (w == Math.floor(w)) {
            return String.valueOf((long) w);
        }
        return String.valueOf(w);
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            }
            focus.clearFocus();
        }
    }

    private void styleDialog(AlertDialog dialog) {
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
        }
    }

    private void showTextDialog(String title, String hint, String initial,
                                String positiveLabel, TextCallback callback) {
        final EditText input = new EditText(this);
        input.setHint(hint);
        input.setText(initial);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setSelection(input.getText().length());

        FrameLayout container = new FrameLayout(this);
        container.setPadding(dp(20), dp(8), dp(20), 0);
        container.addView(input, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(container)
                .setPositiveButton(positiveLabel,
                        (d, which) -> callback.onText(input.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
        styleDialog(dialog);
    }

    private void showConfirmDialog(String title, String message,
                                   String positiveLabel, Runnable onConfirm) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveLabel, (d, which) -> onConfirm.run())
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
        styleDialog(dialog);
    }

    // ---------- Days ----------
    private void promptAddDay() {
        showTextDialog("New training day", "Day name (e.g. Push)", "", "Add", text -> {
            Day day = new Day();
            day.name = text.isEmpty() ? "Day " + (days.size() + 1) : text;
            days.add(day);
            selectedDay = days.size() - 1;
            hideForm();
            saveData();
            refreshAll();

            // Scroll the day bar to the newly added day
            final View bar = (View) llDaysContainer.getParent();
            if (bar instanceof HorizontalScrollView) {
                bar.post(() -> ((HorizontalScrollView) bar).fullScroll(View.FOCUS_RIGHT));
            }
        });
    }

    private void promptRenameDay() {
        final Day day = currentDay();
        if (day == null) return;
        showTextDialog("Rename day", "Day name", day.name, "Save", text -> {
            if (!text.isEmpty()) {
                day.name = text;
                saveData();
                refreshAll();
            }
        });
    }

    private void confirmDeleteDay() {
        final Day day = currentDay();
        if (day == null) return;
        showConfirmDialog("Delete day",
                "Delete \"" + day.name + "\" and all its exercises? "
                        + "Their history stays in Training Analysis.",
                "Delete", () -> {
                    days.remove(selectedDay);
                    if (days.isEmpty()) {
                        selectedDay = -1;
                    } else if (selectedDay >= days.size()) {
                        selectedDay = days.size() - 1;
                    }
                    hideForm();
                    saveData();
                    refreshAll();
                });
    }

    private void selectDay(int index) {
        if (index == selectedDay) return;
        selectedDay = index;
        hideForm();
        saveData();
        renderDays();
        renderDayContent();
        svContent.scrollTo(0, 0);
    }

    // ---------- Rendering ----------
    private void refreshAll() {
        renderDays();
        renderDayContent();
    }

    private void renderDays() {
        llDaysContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < days.size(); i++) {
            final int index = i;
            TextView chip = (TextView) inflater.inflate(
                    R.layout.item_day_chip, llDaysContainer, false);
            chip.setText(days.get(i).name);
            if (i == selectedDay) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected);
                chip.setTextColor(Color.WHITE);
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip);
                chip.setTextColor(getColor(R.color.ts_primary));
            }
            chip.setOnClickListener(v -> selectDay(index));
            llDaysContainer.addView(chip);
        }
    }

    private void renderDayContent() {
        Day day = currentDay();
        boolean hasDay = day != null;

        llEmptyNoDays.setVisibility(hasDay ? View.GONE : View.VISIBLE);
        llDayHeader.setVisibility(hasDay ? View.VISIBLE : View.GONE);
        svContent.setVisibility(hasDay ? View.VISIBLE : View.GONE);
        fabAddExercise.setVisibility(hasDay ? View.VISIBLE : View.GONE);

        if (!hasDay) {
            cardAddExercise.setVisibility(View.GONE);
            editingIndex = -1;
            return;
        }

        tvDayName.setText(day.name);
        tvExerciseCount.setText("Exercises: " + day.exercises.size());
        renderExercises(day);
        updateEmptyStates();
    }

    private void renderExercises(Day day) {
        llExercisesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < day.exercises.size(); i++) {
            final int index = i;
            final Exercise ex = day.exercises.get(i);
            View card = inflater.inflate(R.layout.item_exercise, llExercisesContainer, false);

            ((TextView) card.findViewById(R.id.tvExerciseName)).setText(ex.name);

            TextView tvDesc = card.findViewById(R.id.tvExerciseDescription);
            if (ex.description.isEmpty()) {
                tvDesc.setVisibility(View.GONE);
            } else {
                tvDesc.setVisibility(View.VISIBLE);
                tvDesc.setText(ex.description);
            }

            ((TextView) card.findViewById(R.id.tvSets))
                    .setText("Sets: " + (ex.sets > 0 ? ex.sets : "-"));
            ((TextView) card.findViewById(R.id.tvReps))
                    .setText("Reps: " + (ex.reps > 0 ? ex.reps : "-"));
            ((TextView) card.findViewById(R.id.tvWeight))
                    .setText(ex.weight > 0 ? formatWeight(ex.weight) + " kg" : "- kg");

            card.findViewById(R.id.btnEditExercise).setOnClickListener(v -> showForm(index));
            card.findViewById(R.id.btnDeleteExercise).setOnClickListener(v ->
                    showConfirmDialog("Delete exercise",
                            "Delete \"" + ex.name + "\"? Its history stays in Training Analysis.",
                            "Delete", () -> {
                                day.exercises.remove(index);
                                if (editingIndex == index) {
                                    hideForm();
                                } else if (editingIndex > index) {
                                    editingIndex--;
                                }
                                saveData();
                                renderDayContent();
                            }));

            llExercisesContainer.addView(card);
        }
    }

    private void updateEmptyStates() {
        Day day = currentDay();
        boolean noExercises = day != null && day.exercises.isEmpty();
        boolean formOpen = cardAddExercise.getVisibility() == View.VISIBLE;
        llEmptyNoExercises.setVisibility(noExercises && !formOpen ? View.VISIBLE : View.GONE);
    }

    // ---------- Exercise form ----------
    private void showForm(int exerciseIndex) {
        Day day = currentDay();
        if (day == null) return;

        editingIndex = exerciseIndex;
        clearFormFields();

        if (exerciseIndex >= 0 && exerciseIndex < day.exercises.size()) {
            Exercise ex = day.exercises.get(exerciseIndex);
            tvFormTitle.setText("Edit exercise");
            etExerciseName.setText(ex.name);
            etExerciseDescription.setText(ex.description);
            etSets.setText(ex.sets > 0 ? String.valueOf(ex.sets) : "");
            etReps.setText(ex.reps > 0 ? String.valueOf(ex.reps) : "");
            etWeight.setText(ex.weight > 0 ? formatWeight(ex.weight) : "");
        } else {
            editingIndex = -1;
            tvFormTitle.setText("New exercise");
        }

        cardAddExercise.setVisibility(View.VISIBLE);
        etExerciseName.requestFocus();
        svContent.post(() -> svContent.smoothScrollTo(0, 0));
        updateEmptyStates();
    }

    private void hideForm() {
        hideKeyboard();
        cardAddExercise.setVisibility(View.GONE);
        editingIndex = -1;
        clearFormFields();
        updateEmptyStates();
    }

    private void clearFormFields() {
        etExerciseName.setText("");
        etExerciseDescription.setText("");
        etSets.setText("");
        etReps.setText("");
        etWeight.setText("");
        etExerciseName.setError(null);
        etSets.setError(null);
        etReps.setError(null);
        etWeight.setError(null);
    }

    private int readInt(EditText field) {
        String s = field.getText().toString().trim();
        if (s.isEmpty()) return 0;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException err) {
            field.setError("Invalid number");
            return -1;
        }
    }

    private double readWeight(EditText field) {
        String s = field.getText().toString().trim().replace(',', '.');
        if (s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException err) {
            field.setError("Invalid number");
            return -1;
        }
    }

    private void saveExercise() {
        Day day = currentDay();
        if (day == null) return;

        String name = etExerciseName.getText().toString().trim();
        if (name.isEmpty()) {
            etExerciseName.setError("Enter the exercise name");
            etExerciseName.requestFocus();
            return;
        }

        int sets = readInt(etSets);
        int reps = readInt(etReps);
        double weight = readWeight(etWeight);
        if (sets < 0 || reps < 0 || weight < 0) {
            return;
        }

        Exercise ex;
        if (editingIndex >= 0 && editingIndex < day.exercises.size()) {
            ex = day.exercises.get(editingIndex);
        } else {
            ex = new Exercise();
            day.exercises.add(ex);
        }
        ex.name = name;
        ex.description = etExerciseDescription.getText().toString().trim();
        ex.sets = sets;
        ex.reps = reps;
        ex.weight = weight;

        saveData();
        // Every save adds a point to the history (and to the Training Analysis chart)
        TrainingRepository.logExercise(this, ex);
        hideForm();
        renderDayContent();
    }

    // ---------- Saving and loading ----------
    private void saveData() {
        TrainingRepository.save(this, days, selectedDay);
    }

    private void loadData() {
        TrainingRepository.TrainingData data = TrainingRepository.load(this);
        days.clear();
        days.addAll(data.days);
        selectedDay = data.selectedDay;
    }
}