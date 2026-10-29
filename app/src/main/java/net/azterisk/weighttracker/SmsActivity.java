package net.azterisk.weighttracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;

public class SmsActivity extends AppCompatActivity {
    private TextView textStatus;
    private EditText editTargetWeight;
    private EditText editUserHeight;
    private TextInputLayout layoutUserHeight;
    private SwitchCompat switchMetric;
    private SwitchCompat switchDailyReminder;
    private Button btnRemoveAds;
    private Button btnExportCsv;
    private Button btnImportCsv;
    private BillingManager billingManager;
    private DatabaseHelper dbHelper;
    private long currentUserId = -1;
    
    public static final String KEY_TARGET_WEIGHT = "targetWeight";
    public static final String KEY_USER_HEIGHT = "userHeight";
    public static final String KEY_DAILY_REMINDER = "dailyReminderEnabled";

    private final ActivityResultLauncher<String> createDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri -> {
                if (uri != null) {
                    exportCsvToUri(uri);
                }
            });

    private final ActivityResultLauncher<String[]> openDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    importCsvFromUri(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GlowUtils.enableEdgeToEdge(this);
        setContentView(R.layout.activity_sms);
        
        // Apply gradient and shadow styling to title text
        GlowUtils.applyNeonGlow(findViewById(R.id.textTitle));

        dbHelper = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
        currentUserId = prefs.getLong(MainActivity.KEY_USER_ID, -1);

        Button btnSaveTarget = findViewById(R.id.btnSaveTarget);
        btnRemoveAds = findViewById(R.id.btnRemoveAds);
        btnExportCsv = findViewById(R.id.btnExportCsv);
        btnImportCsv = findViewById(R.id.btnImportCsv);
        textStatus = findViewById(R.id.textStatus);
        editTargetWeight = findViewById(R.id.editTargetWeight);
        editUserHeight = findViewById(R.id.editUserHeight);
        layoutUserHeight = findViewById(R.id.layoutUserHeight);
        switchMetric = findViewById(R.id.switchMetric);
        switchDailyReminder = findViewById(R.id.switchDailyReminder);
        
        billingManager = new BillingManager(this, this::updatePremiumUI);
        updatePremiumUI(billingManager.isPremiumUser());

        // Handle Metric Switch
        boolean isMetric = prefs.getBoolean(MainActivity.KEY_USE_METRIC, false);
        switchMetric.setChecked(isMetric);
        updateHints(isMetric);
        
        switchMetric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(MainActivity.KEY_USE_METRIC, isChecked).apply();
            updateHints(isChecked);
        });

        // Handle Daily Reminder Switch
        boolean reminderEnabled = prefs.getBoolean(KEY_DAILY_REMINDER, false);
        switchDailyReminder.setChecked(reminderEnabled);
        switchDailyReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_DAILY_REMINDER, isChecked).apply();
            NotificationHelper.scheduleDailyReminder(this, isChecked);
            String msg = isChecked ? "Morning weigh-in reminder set for 8:00 AM" : "Daily reminder turned off";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // Handle Target Weight
        float savedTarget = prefs.getFloat(KEY_TARGET_WEIGHT, -1f);
        if (savedTarget != -1f) {
            if (isMetric) {
                editTargetWeight.setText(String.format(Locale.US, "%.1f", savedTarget * 0.453592));
            } else {
                editTargetWeight.setText(String.format(Locale.US, "%.1f", savedTarget));
            }
        }

        // Handle Height (stored in inches internally)
        float savedHeight = prefs.getFloat(KEY_USER_HEIGHT, -1f);
        if (savedHeight != -1f) {
            if (isMetric) {
                editUserHeight.setText(String.format(Locale.US, "%.1f", savedHeight * 2.54));
            } else {
                editUserHeight.setText(String.format(Locale.US, "%.1f", savedHeight));
            }
        }
        
        btnSaveTarget.setOnClickListener(v -> {
            String targetStr = editTargetWeight.getText().toString().trim();
            String heightStr = editUserHeight.getText().toString().trim();

            SharedPreferences.Editor editor = prefs.edit();

            if (!targetStr.isEmpty()) {
                try {
                    float target = Float.parseFloat(targetStr);
                    if (switchMetric.isChecked()) {
                        target = (float) (target / 0.453592);
                    }
                    editor.putFloat(KEY_TARGET_WEIGHT, target);
                } catch (NumberFormatException ignored) {}
            }

            if (!heightStr.isEmpty()) {
                try {
                    float height = Float.parseFloat(heightStr);
                    if (switchMetric.isChecked()) {
                        height = (float) (height / 2.54);
                    }
                    editor.putFloat(KEY_USER_HEIGHT, height);
                } catch (NumberFormatException ignored) {}
            }

            editor.apply();
            Toast.makeText(this, "Profile & Goals Saved", Toast.LENGTH_SHORT).show();
        });

        // CSV Export & Import Handlers (Zero Permissions Required via SAF)
        btnExportCsv.setOnClickListener(v -> {
            String filename = "weight_tracker_backup_" + System.currentTimeMillis() + ".csv";
            createDocumentLauncher.launch(filename);
        });

        btnImportCsv.setOnClickListener(v -> openDocumentLauncher.launch(new String[]{"text/csv", "text/comma-separated-values", "text/*"}));

        btnRemoveAds.setOnClickListener(v -> billingManager.purchaseRemoveAds(this));
    }

    private void exportCsvToUri(Uri uri) {
        if (currentUserId == -1) return;

        try (OutputStream os = getContentResolver().openOutputStream(uri);
             PrintWriter writer = new PrintWriter(os)) {
            
            writer.println("Date,Weight(lbs)");
            Cursor cursor = dbHelper.getWeightsForUser(currentUserId);
            int count = 0;
            if (cursor.moveToFirst()) {
                do {
                    String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEIGHT_DATE));
                    double weight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEIGHT_VALUE));
                    writer.println(String.format(Locale.US, "%s,%.2f", date, weight));
                    count++;
                } while (cursor.moveToNext());
            }
            cursor.close();
            Toast.makeText(this, "Exported " + count + " entries successfully!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void importCsvFromUri(Uri uri) {
        if (currentUserId == -1) return;

        try (InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            
            String line;
            int count = 0;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (isHeader) {
                    isHeader = false;
                    if (line.toLowerCase().contains("date") || line.toLowerCase().contains("weight")) {
                        continue;
                    }
                }

                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String date = parts[0].trim();
                    try {
                        double weight = Double.parseDouble(parts[1].trim());
                        dbHelper.addWeight(currentUserId, date, weight);
                        count++;
                    } catch (NumberFormatException ignored) {}
                }
            }
            Toast.makeText(this, "Imported " + count + " entries successfully!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePremiumUI(boolean isPremium) {
        if (btnRemoveAds != null) {
            if (isPremium) {
                btnRemoveAds.setText(R.string.remove_ads_unlocked);
                btnRemoveAds.setEnabled(false);
            } else {
                btnRemoveAds.setText(R.string.remove_ads);
                btnRemoveAds.setEnabled(true);
            }
        }
    }

    private void updateHints(boolean isMetric) {
        if (isMetric) {
            editTargetWeight.setHint(R.string.target_weight_hint_kg);
            if (layoutUserHeight != null) {
                layoutUserHeight.setHint("Height (cm for BMI)");
            }
        } else {
            editTargetWeight.setHint(R.string.target_weight_hint);
            if (layoutUserHeight != null) {
                layoutUserHeight.setHint("Height (inches for BMI)");
            }
        }
    }
}
