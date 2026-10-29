package net.azterisk.weighttracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeightListActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private long currentUserId = -1;
    private LinearLayout layoutWeightList;
    private AdView mAdView;
    private BillingManager billingManager;

    // Journey Summary Views
    private View cardJourneySummary;
    private TextView textStartWeight;
    private TextView textCurrentWeight;
    private TextView textTargetWeight;
    private TextView textTotalChange;
    private TextView textBmi;
    private TextView textPaceEta;
    private ProgressBar progressGoal;
    private WeightChartView chartWeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GlowUtils.enableEdgeToEdge(this);
        setContentView(R.layout.activity_weight_list);

        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {});

        mAdView = findViewById(R.id.adView);
        billingManager = new BillingManager(this, isPremium -> updateAdVisibility(isPremium));

        if (billingManager.isPremiumUser()) {
            updateAdVisibility(true);
        } else {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }

        dbHelper = new DatabaseHelper(this);
        layoutWeightList = findViewById(R.id.layoutWeightList);

        // Bind Summary Views
        cardJourneySummary = findViewById(R.id.cardJourneySummary);
        textStartWeight = findViewById(R.id.textStartWeight);
        textCurrentWeight = findViewById(R.id.textCurrentWeight);
        textTargetWeight = findViewById(R.id.textTargetWeight);
        textTotalChange = findViewById(R.id.textTotalChange);
        textBmi = findViewById(R.id.textBmi);
        textPaceEta = findViewById(R.id.textPaceEta);
        progressGoal = findViewById(R.id.progressGoal);
        chartWeight = findViewById(R.id.chartWeight);

        // Apply gradient and shadow styling to header title
        GlowUtils.applyNeonGlow(findViewById(R.id.textHeaderTitle));

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
        currentUserId = prefs.getLong(MainActivity.KEY_USER_ID, -1);

        if (currentUserId == -1) {
            finish(); // Not logged in
            return;
        }

        Button btnAdd = findViewById(R.id.btnAdd);
        ImageButton btnSettings = findViewById(R.id.btnSettings);

        btnAdd.setOnClickListener(v -> startActivity(new Intent(WeightListActivity.this, AddWeightActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(WeightListActivity.this, SmsActivity.class)));
    }

    private void updateAdVisibility(boolean isPremium) {
        if (mAdView != null) {
            mAdView.setVisibility(isPremium ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUserId != -1) {
            loadWeights();
        }
        if (billingManager != null && billingManager.isPremiumUser()) {
            updateAdVisibility(true);
        }
    }

    private void loadWeights() {
        layoutWeightList.removeAllViews();
        Cursor cursor = dbHelper.getWeightsForUser(currentUserId);
        
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
        boolean isMetric = prefs.getBoolean(MainActivity.KEY_USE_METRIC, false);
        String unit = isMetric ? "kg" : "lbs";

        TextView headerWeight = findViewById(R.id.textWeight);
        if (headerWeight != null) {
            headerWeight.setText(isMetric ? R.string.weight_kg : R.string.weight_lbs);
        }

        List<WeightEntry> entries = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEIGHT_ID));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEIGHT_DATE));
                double rawWeight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEIGHT_VALUE));

                double displayWeight = isMetric ? (rawWeight * 0.453592) : rawWeight;
                entries.add(new WeightEntry(id, date, displayWeight));
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (entries.isEmpty()) {
            if (cardJourneySummary != null) {
                cardJourneySummary.setVisibility(View.GONE);
            }
            return;
        }

        if (cardJourneySummary != null) {
            cardJourneySummary.setVisibility(View.VISIBLE);
        }

        // Calculate 7-Day Exponential Moving Average (EMA) for trend smoothing (alpha = 0.25)
        double alpha = 0.25;
        double ema = 0;
        List<Double> chartPoints = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            WeightEntry e = entries.get(i);
            if (i == 0) {
                ema = e.getWeight();
            } else {
                ema = alpha * e.getWeight() + (1.0 - alpha) * ema;
            }
            e.setTrendWeight(ema);
            chartPoints.add(e.getWeight());
        }

        // Target weight from settings
        float targetFloat = prefs.getFloat(SmsActivity.KEY_TARGET_WEIGHT, -1f);
        Double targetVal = null;
        if (targetFloat > 0) {
            targetVal = isMetric ? (targetFloat * 0.453592) : (double) targetFloat;
        }

        // Update Journey Summary card
        WeightEntry firstEntry = entries.get(0);
        WeightEntry latestEntry = entries.get(entries.size() - 1);

        double startWeight = firstEntry.getWeight();
        double currentWeight = latestEntry.getWeight();

        textStartWeight.setText(String.format(Locale.US, "%.1f %s", startWeight, unit));
        textCurrentWeight.setText(String.format(Locale.US, "%.1f %s", currentWeight, unit));

        if (targetVal != null && targetVal > 0) {
            textTargetWeight.setText(String.format(Locale.US, "%.1f %s", targetVal, unit));

            // Calculate progress percentage
            double progressPercent = 0.0;
            if (Math.abs(startWeight - targetVal) > 0.01) {
                if (startWeight > targetVal) {
                    // Weight loss
                    progressPercent = ((startWeight - currentWeight) / (startWeight - targetVal)) * 100.0;
                } else {
                    // Weight gain
                    progressPercent = ((currentWeight - startWeight) / (targetVal - startWeight)) * 100.0;
                }
            }
            int progressClamped = (int) Math.max(0, Math.min(100, Math.round(progressPercent)));
            progressGoal.setProgress(progressClamped);

            double remaining = Math.abs(currentWeight - targetVal);
            textPaceEta.setText(String.format(Locale.US, "%.1f %s remaining (%d%%)", remaining, unit, progressClamped));
        } else {
            textTargetWeight.setText("--");
            progressGoal.setProgress(0);
            textPaceEta.setText("Set target in Settings");
        }

        double totalDiff = currentWeight - startWeight;
        double percentDiff = (startWeight > 0) ? ((totalDiff / startWeight) * 100.0) : 0;

        if (Math.abs(totalDiff) < 0.05) {
            textTotalChange.setText("No net change");
            textTotalChange.setTextColor(Color.parseColor("#8E9AA8"));
        } else if (totalDiff < 0) {
            textTotalChange.setText(String.format(Locale.US, "📉 -%.1f %s (%.1f%%)", Math.abs(totalDiff), unit, Math.abs(percentDiff)));
            textTotalChange.setTextColor(Color.parseColor("#00F5D4")); // Neon teal
        } else {
            textTotalChange.setText(String.format(Locale.US, "📈 +%.1f %s (+%.1f%%)", totalDiff, unit, percentDiff));
            textTotalChange.setTextColor(Color.parseColor("#FFD166")); // Amber
        }

        // Local BMI Calculation if Height is set (Stored in inches internally)
        float userHeightInches = prefs.getFloat(SmsActivity.KEY_USER_HEIGHT, -1f);
        if (textBmi != null) {
            if (userHeightInches > 0 && currentWeight > 0) {
                double weightInLbs = isMetric ? (currentWeight / 0.453592) : currentWeight;
                double bmi = (703.0 * weightInLbs) / Math.pow(userHeightInches, 2);
                String category = (bmi < 18.5) ? "Under" : ((bmi < 25.0) ? "Normal" : ((bmi < 30.0) ? "Over" : "Obese"));
                textBmi.setText(String.format(Locale.US, "BMI: %.1f (%s)", bmi, category));
                textBmi.setVisibility(View.VISIBLE);
            } else {
                textBmi.setVisibility(View.GONE);
            }
        }

        // Pass to chart
        if (chartWeight != null) {
            chartWeight.setData(chartPoints, targetVal, isMetric);
        }

        // Render rows in reverse chronological order (newest first)
        for (int i = entries.size() - 1; i >= 0; i--) {
            WeightEntry entry = entries.get(i);
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_weight, layoutWeightList, false);

            TextView textDate = itemView.findViewById(R.id.textDate);
            TextView textTrend = itemView.findViewById(R.id.textTrend);
            TextView textWeight = itemView.findViewById(R.id.textWeight);
            View btnEdit = itemView.findViewById(R.id.btnEdit);
            View btnDelete = itemView.findViewById(R.id.btnDelete);

            textDate.setText(entry.getDate());
            textWeight.setText(String.format(Locale.US, "%.1f", entry.getWeight()));
            
            if (textTrend != null) {
                textTrend.setText(String.format(Locale.US, "Trend: %.1f %s", entry.getTrendWeight(), unit));
            }

            final long entryId = entry.getId();
            final String entryDate = entry.getDate();
            final double entryWeight = entry.getWeight();

            btnDelete.setOnClickListener(v -> {
                dbHelper.deleteWeight(entryId);
                loadWeights();
            });

            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(WeightListActivity.this, AddWeightActivity.class);
                intent.putExtra("entryId", entryId);
                intent.putExtra("date", entryDate);
                intent.putExtra("weight", entryWeight);
                startActivity(intent);
            });

            layoutWeightList.addView(itemView);
        }
    }
}
