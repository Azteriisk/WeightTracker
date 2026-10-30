package net.azterisk.weighttracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private TextInputEditText editUsername;
    private TextInputEditText editPassword;

    public static final String PREF_NAME = "WeightTrackerPrefs";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_USE_METRIC = "useMetric";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GlowUtils.enableEdgeToEdge(this);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);

        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);

        // Login button handler
        btnLogin.setOnClickListener(v -> {
            String username = editUsername.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                username = "demo";
                password = "password";
            }

            if (!dbHelper.checkUserExists(username)) {
                dbHelper.registerUser(username, password);
            }

            if (dbHelper.authenticateUser(username, password)) {
                long userId = dbHelper.getUserId(username);
                saveUserSession(userId);

                // Populate sample weight history if empty
                Cursor cursor = dbHelper.getWeightsForUser(userId);
                if (!cursor.moveToFirst()) {
                    dbHelper.addWeight(userId, "10/22/2026", 182.5);
                    dbHelper.addWeight(userId, "10/24/2026", 181.2);
                    dbHelper.addWeight(userId, "10/25/2026", 180.0);
                }
                cursor.close();

                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, WeightListActivity.class));
            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
            }
        });

        // Register button handler
        btnRegister.setOnClickListener(v -> {
            String username = editUsername.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                username = "demo";
                password = "password";
            }

            if (!dbHelper.checkUserExists(username)) {
                dbHelper.registerUser(username, password);
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Username already exists. Logging in...", Toast.LENGTH_SHORT).show();
            }

            long userId = dbHelper.getUserId(username);
            saveUserSession(userId);

            Cursor cursor = dbHelper.getWeightsForUser(userId);
            if (!cursor.moveToFirst()) {
                dbHelper.addWeight(userId, "10/22/2026", 182.5);
                dbHelper.addWeight(userId, "10/24/2026", 181.2);
                dbHelper.addWeight(userId, "10/25/2026", 180.0);
            }
            cursor.close();

            startActivity(new Intent(MainActivity.this, WeightListActivity.class));
        });
    }

    private void saveUserSession(long userId) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_USER_ID, userId);
        editor.apply();
    }
}
