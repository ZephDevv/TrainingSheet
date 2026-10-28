package com.example.trainingapp;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class BmiActivity extends AppCompatActivity {

    private EditText etHeight, etWeight;
    private LinearLayout cardResult;
    private TextView tvBmiValue, tvBmiCategory, tvBmiRange;
    private LinearLayout rowUnderweight, rowNormal, rowOverweight, rowObese;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // The app is designed in black and white, so always use the light theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bmi);

        // Keep content clear of system bars and the keyboard
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bmiRoot), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        bindViews();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCalculate).setOnClickListener(v -> calculate());
        findViewById(R.id.btnReset).setOnClickListener(v -> reset());

        // "Done" on the keyboard also calculates
        etWeight.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                calculate();
                return true;
            }
            return false;
        });
    }

    private void bindViews() {
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etBmiWeight);
        cardResult = findViewById(R.id.cardResult);
        tvBmiValue = findViewById(R.id.tvBmiValue);
        tvBmiCategory = findViewById(R.id.tvBmiCategory);
        tvBmiRange = findViewById(R.id.tvBmiRange);
        rowUnderweight = findViewById(R.id.rowUnderweight);
        rowNormal = findViewById(R.id.rowNormal);
        rowOverweight = findViewById(R.id.rowOverweight);
        rowObese = findViewById(R.id.rowObese);
    }

    /** Reads a number from the field (accepts comma or dot). Returns NaN if empty or invalid. */
    private double readNumber(EditText field) {
        String s = field.getText().toString().trim().replace(',', '.');
        if (s.isEmpty()) return Double.NaN;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException err) {
            return Double.NaN;
        }
    }

    private void calculate() {
        etHeight.setError(null);
        etWeight.setError(null);

        double heightCm = readNumber(etHeight);
        double weightKg = readNumber(etWeight);

        // Validation
        if (Double.isNaN(heightCm) || heightCm < 50 || heightCm > 250) {
            etHeight.setError("Enter a height between 50 and 250 cm");
            etHeight.requestFocus();
            return;
        }
        if (Double.isNaN(weightKg) || weightKg < 20 || weightKg > 300) {
            etWeight.setError("Enter a weight between 20 and 300 kg");
            etWeight.requestFocus();
            return;
        }

        hideKeyboard();

        // BMI = weight (kg) / height (m)^2
        double heightM = heightCm / 100.0;
        double bmi = weightKg / (heightM * heightM);
        double bmiRounded = Math.round(bmi * 10.0) / 10.0; // category uses the displayed value

        String category;
        LinearLayout activeRow;
        if (bmiRounded < 18.5) {
            category = "Underweight";
            activeRow = rowUnderweight;
        } else if (bmiRounded < 25.0) {
            category = "Normal weight";
            activeRow = rowNormal;
        } else if (bmiRounded < 30.0) {
            category = "Overweight";
            activeRow = rowOverweight;
        } else {
            category = "Obesity";
            activeRow = rowObese;
        }

        // Weight range that gives a BMI of 18.5 - 24.9 at this height
        double minWeight = 18.5 * heightM * heightM;
        double maxWeight = 24.9 * heightM * heightM;

        tvBmiValue.setText(String.format(Locale.US, "%.1f", bmiRounded));
        tvBmiCategory.setText(category);
        tvBmiRange.setText(String.format(Locale.US,
                "Normal BMI range for your height: %.1f - %.1f kg", minWeight, maxWeight));
        cardResult.setVisibility(View.VISIBLE);

        highlightRow(rowUnderweight, activeRow == rowUnderweight);
        highlightRow(rowNormal, activeRow == rowNormal);
        highlightRow(rowOverweight, activeRow == rowOverweight);
        highlightRow(rowObese, activeRow == rowObese);
    }

    private void reset() {
        etHeight.setText("");
        etWeight.setText("");
        etHeight.setError(null);
        etWeight.setError(null);
        cardResult.setVisibility(View.GONE);
        highlightRow(rowUnderweight, false);
        highlightRow(rowNormal, false);
        highlightRow(rowOverweight, false);
        highlightRow(rowObese, false);
        etHeight.requestFocus();
    }

    /** Marks the current category row in black with white text. */
    private void highlightRow(LinearLayout row, boolean active) {
        if (active) {
            row.setBackgroundResource(R.drawable.bg_chip_selected);
            ((TextView) row.getChildAt(0)).setTextColor(Color.WHITE);
            ((TextView) row.getChildAt(1)).setTextColor(Color.WHITE);
        } else {
            row.setBackground(null);
            ((TextView) row.getChildAt(0)).setTextColor(getColor(R.color.ts_text));
            ((TextView) row.getChildAt(1)).setTextColor(getColor(R.color.ts_text_secondary));
        }
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
}