package net.azterisk.weighttracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WeightChartView extends View {

    private final List<Double> dataPoints = new ArrayList<>();
    private Double targetWeight = null;
    private boolean isMetric = false;

    private Paint linePaint;
    private Paint fillPaint;
    private Paint gridPaint;
    private Paint targetPaint;
    private Paint dotPaint;
    private Paint textPaint;

    private Path linePath = new Path();
    private Path fillPath = new Path();

    public WeightChartView(Context context) {
        super(context);
        init();
    }

    public WeightChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeightChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(6f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#222831"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2f);

        targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        targetPaint.setColor(Color.parseColor("#FFD166"));
        targetPaint.setStyle(Paint.Style.STROKE);
        targetPaint.setStrokeWidth(3f);
        targetPaint.setPathEffect(new DashPathEffect(new float[]{12f, 10f}, 0));

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.parseColor("#00F5D4"));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#8E9AA8"));
        textPaint.setTextSize(26f);
    }

    public void setData(List<Double> weights, @Nullable Double target, boolean metric) {
        dataPoints.clear();
        if (weights != null) {
            dataPoints.addAll(weights);
        }
        this.targetWeight = target;
        this.isMetric = metric;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0 || dataPoints.isEmpty()) {
            return;
        }

        float padLeft = 40f;
        float padRight = 50f;
        float padTop = 40f;
        float padBottom = 40f;

        float chartWidth = width - padLeft - padRight;
        float chartHeight = height - padTop - padBottom;

        if (chartWidth <= 0 || chartHeight <= 0) {
            return;
        }

        // Calculate min and max
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (double w : dataPoints) {
            if (w < min) min = w;
            if (w > max) max = w;
        }

        if (targetWeight != null && targetWeight > 0) {
            if (targetWeight < min) min = targetWeight;
            if (targetWeight > max) max = targetWeight;
        }

        // Add 5% padding to min/max
        double range = max - min;
        if (range <= 0.001) {
            range = 5.0;
            min -= 2.5;
            max += 2.5;
        } else {
            min -= range * 0.08;
            max += range * 0.08;
            range = max - min;
        }

        // Draw horizontal grid lines (3 lines)
        for (int i = 0; i <= 3; i++) {
            float y = padTop + (chartHeight * i / 3f);
            canvas.drawLine(padLeft, y, width - padRight, y, gridPaint);
        }

        // Target weight dotted guideline
        if (targetWeight != null && targetWeight > 0) {
            float targetY = (float) (padTop + chartHeight * (1.0 - (targetWeight - min) / range));
            if (targetY >= padTop && targetY <= padTop + chartHeight) {
                canvas.drawLine(padLeft, targetY, width - padRight, targetY, targetPaint);
                String unit = isMetric ? "kg" : "lbs";
                String targetLabel = String.format(java.util.Locale.US, "Goal: %.1f %s", targetWeight, unit);
                canvas.drawText(targetLabel, width - padRight - 160f, targetY - 10f, textPaint);
            }
        }

        if (dataPoints.size() == 1) {
            // Single point: draw a central dot and label
            float cx = padLeft + chartWidth / 2f;
            float cy = padTop + chartHeight / 2f;
            canvas.drawCircle(cx, cy, 10f, dotPaint);
            String unit = isMetric ? "kg" : "lbs";
            canvas.drawText(String.format(java.util.Locale.US, "%.1f %s", dataPoints.get(0), unit), cx + 16f, cy - 16f, textPaint);
            return;
        }

        // Multiple points: create smooth paths
        linePath.reset();
        fillPath.reset();

        int n = dataPoints.size();
        float stepX = chartWidth / (n - 1);

        float firstX = padLeft;
        float firstY = (float) (padTop + chartHeight * (1.0 - (dataPoints.get(0) - min) / range));

        linePath.moveTo(firstX, firstY);
        fillPath.moveTo(firstX, padTop + chartHeight);
        fillPath.lineTo(firstX, firstY);

        float prevX = firstX;
        float prevY = firstY;

        for (int i = 1; i < n; i++) {
            float curX = padLeft + i * stepX;
            float curY = (float) (padTop + chartHeight * (1.0 - (dataPoints.get(i) - min) / range));

            // Smooth cubic Bezier interpolation
            float midX1 = (prevX + curX) / 2f;
            float midY1 = prevY;
            float midX2 = (prevX + curX) / 2f;
            float midY2 = curY;

            linePath.cubicTo(midX1, midY1, midX2, midY2, curX, curY);
            fillPath.cubicTo(midX1, midY1, midX2, midY2, curX, curY);

            prevX = curX;
            prevY = curY;
        }

        fillPath.lineTo(prevX, padTop + chartHeight);
        fillPath.close();

        // Shaders
        linePaint.setShader(new LinearGradient(
                padLeft, 0, width - padRight, 0,
                Color.parseColor("#00F5D4"), Color.parseColor("#00BBF9"),
                Shader.TileMode.CLAMP
        ));

        fillPaint.setShader(new LinearGradient(
                0, padTop, 0, padTop + chartHeight,
                Color.parseColor("#3300F5D4"), Color.parseColor("#00000000"),
                Shader.TileMode.CLAMP
        ));

        // Draw fill, then line
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        // Draw dot on the last point
        canvas.drawCircle(prevX, prevY, 9f, dotPaint);
    }
}
