package net.azterisk.weighttracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

public class AddWeightActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private long currentUserId = -1;
    private long entryId = -1;
    
    private EditText editDate;
    private EditText editWeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GlowUtils.enableEdgeToEdge(this);
        setContentView(R.layout.activity_add_weight);

        dbHelper = new DatabaseHelper(this);
        
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
        currentUserId = prefs.getLong(MainActivity.KEY_USER_ID, -1);
        
        if (currentUserId == -1) {
            finish();
            return;
        }

        editDate = findViewById(R.id.editDate);
        editWeight = findViewById(R.id.editWeight);
        Button btnSave = findViewById(R.id.btnSave);

        // Apply gradient and shadow styling to header title
        GlowUtils.applyNeonGlow(findViewById(R.id.textTitle));

        // Set up the DatePickerDialog
        editDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(AddWeightActivity.this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        String formattedDate = String.format(Locale.US, "%02d/%02d/%04d", monthOfYear + 1, dayOfMonth, year1);
                        editDate.setText(formattedDate);
                    }, year, month, day);
            datePickerDialog.show();
        });

        // Check if editing an existing entry
        Intent intent = getIntent();
        if (intent.hasExtra("entryId")) {
            entryId = intent.getLongExtra("entryId", -1);
            String existingDate = intent.getStringExtra("date");
            double existingWeight = intent.getDoubleExtra("weight", 0);

            editDate.setText(existingDate);
            editWeight.setText(String.format(Locale.US, "%.1f", existingWeight));
            btnSave.setText(R.string.save);
        } else {
            // Default to today's date for a new entry
            final Calendar c = Calendar.getInstance();
            String today = String.format(Locale.US, "%02d/%02d/%04d",
                    c.get(Calendar.MONTH) + 1,
                    c.get(Calendar.DAY_OF_MONTH),
                    c.get(Calendar.YEAR));
            editDate.setText(today);
        }

        btnSave.setOnClickListener(v -> saveWeightEntry());
    }

    private void saveWeightEntry() {
        String date = editDate.getText().toString().trim();
        String weightStr = editWeight.getText().toString().trim();
        
        if (date.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(this, "Please enter both date and weight", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            double weight = Double.parseDouble(weightStr);
            boolean success;
            
            SharedPreferences prefSettings = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
            boolean isMetricSetting = prefSettings.getBoolean(MainActivity.KEY_USE_METRIC, false);
            
            // Always convert to lbs for internal database storage
            if (isMetricSetting) {
                weight = weight / 0.453592;
            }
            
            if (entryId == -1) {
                success = dbHelper.addWeight(currentUserId, date, weight);
                if (success) {
                    Toast.makeText(this, "Weight added successfully", Toast.LENGTH_SHORT).show();
                    checkGoalAndSendNotification(weight);
                } else {
                    Toast.makeText(this, "Failed to add weight", Toast.LENGTH_SHORT).show();
                }
            } else {
                success = dbHelper.updateWeight(entryId, date, weight);
                if (success) {
                    Toast.makeText(this, "Weight updated successfully", Toast.LENGTH_SHORT).show();
                    checkGoalAndSendNotification(weight);
                } else {
                    Toast.makeText(this, "Failed to update weight", Toast.LENGTH_SHORT).show();
                }
            }
            
            if (success) {
                finish();
            }
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid weight format", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void checkGoalAndSendNotification(double currentWeight) {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
        float targetWeight = prefs.getFloat(SettingsActivity.KEY_TARGET_WEIGHT, -1f);
        
        if (targetWeight != -1f && currentWeight <= targetWeight) {
            String message = "Congratulations! You have reached your target weight of " + String.format(Locale.US, "%.1f", targetWeight) + " lbs!";
            NotificationHelper.sendGoalNotification(this, message);
        }
    }
}
