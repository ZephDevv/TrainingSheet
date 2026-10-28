package com.example.trainingapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Black-and-white line chart. Every value is one point, spaced evenly in the order
 * it was saved. The X axis shows the date and time of the points. Tap or drag to
 * see the exact value and time.
 */
public class LineChartView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd.MM", Locale.US);
    private final SimpleDateFormat dateYearFmt = new SimpleDateFormat("dd.MM.yy", Locale.US);
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.US);
    private final SimpleDateFormat fullFmt = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US);
    private final Path path = new Path();
    private final RectF box = new RectF();

    private long[] times = new long[0];
    private double[] values = new double[0];
    private String unit = "";
    private double yMin = 0;
    private double yMax = 1;
    private int selected = -1;
    private boolean multiYear = false;

    private float plotLeft, plotTop, plotWidth, plotHeight;
    private float downX, downY;

    public LineChartView(Context context) {
        super(context);
        init();
    }

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setColor(Color.BLACK);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2.5f));
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        pointPaint.setColor(Color.BLACK);
        pointPaint.setStyle(Paint.Style.FILL);

        gridPaint.setColor(0xFFE0E0E0);
        gridPaint.setStrokeWidth(dp(1));

        tickPaint.setColor(0xFF9E9E9E);
        tickPaint.setStrokeWidth(dp(1));

        guidePaint.setColor(0xFFBDBDBD);
        guidePaint.setStrokeWidth(dp(1));

        labelPaint.setColor(0xFF616161);
        labelPaint.setTextSize(sp(11));

        tipBgPaint.setColor(Color.BLACK);
        tipBgPaint.setStyle(Paint.Style.FILL);

        tipTextPaint.setColor(Color.WHITE);
        tipTextPaint.setTextSize(sp(12));
        tipTextPaint.setFakeBoldText(true);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v,
                getResources().getDisplayMetrics());
    }

    /** Sets the data: one time (ms, oldest first) and one value per point. */
    public void setData(long[] times, double[] values, String unit) {
        this.times = times;
        this.values = values;
        this.unit = unit;
        this.selected = values.length - 1;

        if (values.length > 0) {
            double min = values[0];
            double max = values[0];
            for (double v : values) {
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
            double range = max - min;
            double pad = range < 1e-9 ? Math.max(1.0, Math.abs(max) * 0.1) : range * 0.15;
            yMin = min - pad;
            yMax = max + pad;
            if (min >= 0 && yMin < 0) yMin = 0;

            Calendar a = Calendar.getInstance();
            Calendar b = Calendar.getInstance();
            a.setTimeInMillis(times[0]);
            b.setTimeInMillis(times[times.length - 1]);
            multiYear = a.get(Calendar.YEAR) != b.get(Calendar.YEAR);
        }
        invalidate();
    }

    public static String formatNumber(double v) {
        if (Math.abs(v - Math.rint(v)) < 0.05) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.format(Locale.US, "%.1f", v);
    }

    private void updatePlotBounds() {
        plotLeft = dp(48);
        plotTop = dp(28);
        plotWidth = getWidth() - plotLeft - dp(24);
        plotHeight = getHeight() - plotTop - dp(48);
    }

    /** Points are spaced evenly, in the order they were saved. */
    private float xFor(int i) {
        int n = values.length;
        if (n == 1) {
            return plotLeft + plotWidth / 2f;
        }
        return plotLeft + plotWidth * i / (float) (n - 1);
    }

    private float yFor(double v) {
        double frac = (v - yMin) / (yMax - yMin);
        return plotTop + plotHeight - (float) (frac * plotHeight);
    }

    private String formatAxis(double v) {
        double span = yMax - yMin;
        if (span >= 8) return String.format(Locale.US, "%.0f", v);
        if (span >= 0.8) return String.format(Locale.US, "%.1f", v);
        return String.format(Locale.US, "%.2f", v);
    }

    private void drawXLabel(Canvas canvas, int i, float x, float axisY) {
        Date d = new Date(times[i]);
        String top = (multiYear ? dateYearFmt : dateFmt).format(d);
        String bottom = timeFmt.format(d);
        float y1 = axisY + dp(18);
        canvas.drawText(top, x, y1, labelPaint);
        canvas.drawText(bottom, x, y1 + sp(11) + dp(2), labelPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = values.length;
        if (n == 0) return;
        updatePlotBounds();
        if (plotWidth <= 0 || plotHeight <= 0) return;

        float axisY = plotTop + plotHeight;

        // Horizontal grid and Y labels
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 4; i++) {
            double value = yMin + (yMax - yMin) * i / 4.0;
            float y = yFor(value);
            canvas.drawLine(plotLeft, y, plotLeft + plotWidth, y, gridPaint);
            canvas.drawText(formatAxis(value), plotLeft - dp(6), y + sp(11) / 3f, labelPaint);
        }

        // X axis: a tick for every point, date and time labels where there is room
        labelPaint.setTextAlign(Paint.Align.CENTER);
        float labelWidth = labelPaint.measureText("00.00.00") + dp(10);
        float lastPointX = xFor(n - 1);
        float lastLabelX = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            float x = xFor(i);
            canvas.drawLine(x, axisY, x, axisY + dp(4), tickPaint);

            boolean draw;
            if (n == 1 || i == 0 || i == n - 1) {
                draw = true;
            } else {
                draw = (x - lastLabelX >= labelWidth) && (lastPointX - x >= labelWidth);
            }
            if (draw) {
                drawXLabel(canvas, i, x, axisY);
                lastLabelX = x;
            }
        }

        // Guide line at the selected point
        if (selected >= 0 && selected < n) {
            float gx = xFor(selected);
            canvas.drawLine(gx, plotTop, gx, axisY, guidePaint);
        }

        // Line
        if (n > 1) {
            path.reset();
            path.moveTo(xFor(0), yFor(values[0]));
            for (int i = 1; i < n; i++) {
                path.lineTo(xFor(i), yFor(values[i]));
            }
            canvas.drawPath(path, linePaint);
        }

        // Points (smaller when there are many)
        float radius = n > 40 ? dp(2.5f) : dp(4);
        for (int i = 0; i < n; i++) {
            canvas.drawCircle(xFor(i), yFor(values[i]), radius, pointPaint);
        }

        // Selected point and tooltip
        if (selected >= 0 && selected < n) {
            float px = xFor(selected);
            float py = yFor(values[selected]);

            pointPaint.setColor(Color.BLACK);
            canvas.drawCircle(px, py, dp(7), pointPaint);
            pointPaint.setColor(Color.WHITE);
            canvas.drawCircle(px, py, dp(3), pointPaint);
            pointPaint.setColor(Color.BLACK);

            String text = formatNumber(values[selected]) + " " + unit + " - "
                    + fullFmt.format(new Date(times[selected]));
            float textW = tipTextPaint.measureText(text);
            float padX = dp(8);
            float padY = dp(6);
            float boxW = textW + padX * 2;
            float boxH = sp(12) + padY * 2;

            float left = px - boxW / 2f;
            left = Math.max(dp(4), Math.min(left, getWidth() - boxW - dp(4)));
            float top = py - dp(14) - boxH;
            if (top < dp(2)) top = py + dp(14);

            box.set(left, top, left + boxW, top + boxH);
            canvas.drawRoundRect(box, dp(6), dp(6), tipBgPaint);
            canvas.drawText(text, left + padX, top + padY + sp(12) * 0.8f, tipTextPaint);
        }
    }

    private void selectNearest(float x) {
        updatePlotBounds();
        int best = -1;
        float bestDist = Float.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            float d = Math.abs(xFor(i) - x);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        if (best != selected) {
            selected = best;
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (values.length == 0) return super.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                selectNearest(event.getX());
                return true;
            case MotionEvent.ACTION_MOVE:
                // Horizontal drag moves the selection instead of scrolling the page
                if (Math.abs(event.getX() - downX) > Math.abs(event.getY() - downY)) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                selectNearest(event.getX());
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}