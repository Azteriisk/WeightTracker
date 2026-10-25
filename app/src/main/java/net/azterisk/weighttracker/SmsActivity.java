package net.azterisk.weighttracker;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SmsActivity extends AppCompatActivity {
    private TextView textStatus;
    private EditText editTargetWeight;
    private SwitchCompat switchMetric;
    private static final int SMS_PERMISSION_CODE = 100;
    
    public static final String KEY_TARGET_WEIGHT = "targetWeight";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GlowUtils.enableEdgeToEdge(this);
        setContentView(R.layout.activity_sms);
        
        // Apply neon gradient & glow to title text
        GlowUtils.applyNeonGlow(findViewById(R.id.textTitle));

        Button btnEnableSms = findViewById(R.id.btnEnableSms);
        Button btnSaveTarget = findViewById(R.id.btnSaveTarget);
        textStatus = findViewById(R.id.textStatus);
        editTargetWeight = findViewById(R.id.editTargetWeight);
        switchMetric = findViewById(R.id.switchMetric);
        
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
        
        // Handle Metric Switch
        boolean isMetric = prefs.getBoolean(MainActivity.KEY_USE_METRIC, false);
        switchMetric.setChecked(isMetric);
        updateHint(isMetric);
        
        switchMetric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(MainActivity.KEY_USE_METRIC, isChecked).apply();
            updateHint(isChecked);
        });

        // Handle Target Weight
        float savedTarget = prefs.getFloat(KEY_TARGET_WEIGHT, -1f);
        if (savedTarget != -1f) {
            // Display appropriately based on metric setting
            if (isMetric) {
                editTargetWeight.setText(String.format(java.util.Locale.US, "%.1f", savedTarget * 0.453592));
            } else {
                editTargetWeight.setText(String.format(java.util.Locale.US, "%.1f", savedTarget));
            }
        }
        
        btnSaveTarget.setOnClickListener(v -> {
            String targetStr = editTargetWeight.getText().toString().trim();
            if (!targetStr.isEmpty()) {
                try {
                    float target = Float.parseFloat(targetStr);
                    // Always store as lbs internally
                    if (switchMetric.isChecked()) {
                        target = (float) (target / 0.453592);
                    }
                    prefs.edit().putFloat(KEY_TARGET_WEIGHT, target).apply();
                    Toast.makeText(this, "Target Weight Saved", Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid weight format", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnEnableSms.setOnClickListener(v -> checkSmsPermission());
        
        // Update status text based on current permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
                == PackageManager.PERMISSION_GRANTED) {
            textStatus.setText("Status: SMS Notifications Enabled.");
        }
    }

    private void updateHint(boolean isMetric) {
        if (isMetric) {
            editTargetWeight.setHint(R.string.target_weight_hint_kg);
        } else {
            editTargetWeight.setHint(R.string.target_weight_hint);
        }
    }

    private void checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
                == PackageManager.PERMISSION_GRANTED) {
            textStatus.setText("Status: Permission Already Granted. SMS Notifications Enabled.");
            Toast.makeText(this, "SMS Permission is already granted.", Toast.LENGTH_SHORT).show();
        } else {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                textStatus.setText("Status: Permission Granted. SMS Notifications Enabled.");
                Toast.makeText(this, "SMS Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                textStatus.setText("Status: Permission Denied. SMS Notifications Disabled.");
                Toast.makeText(this, "SMS Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
