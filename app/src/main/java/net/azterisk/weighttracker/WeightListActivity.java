package net.azterisk.weighttracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class WeightListActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private long currentUserId = -1;
    private LinearLayout layoutWeightList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GlowUtils.enableEdgeToEdge(this);
        setContentView(R.layout.activity_weight_list);

        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {});

        // Load an ad into the AdMob banner view
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        dbHelper = new DatabaseHelper(this);
        layoutWeightList = findViewById(R.id.layoutWeightList);

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

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUserId != -1) {
            loadWeights();
        }
    }

    private void loadWeights() {
        // Clear the current views to prepare for reloading
        layoutWeightList.removeAllViews();
        Cursor cursor = dbHelper.getWeightsForUser(currentUserId);
        
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
        boolean isMetric = prefs.getBoolean(MainActivity.KEY_USE_METRIC, false);

        // Update the header label dynamically based on settings
        TextView headerWeight = findViewById(R.id.textWeight);
        if (headerWeight != null) {
            headerWeight.setText(isMetric ? R.string.weight_kg : R.string.weight_lbs);
        }

        // Iterate through the database cursor and add a row for each entry
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEIGHT_ID));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEIGHT_DATE));
                double weight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEIGHT_VALUE));

                // Convert to kg if metric is selected
                if (isMetric) {
                    weight = weight * 0.453592;
                }

                // Inflate the custom row layout
                View itemView = LayoutInflater.from(this).inflate(R.layout.item_weight, layoutWeightList, false);

                TextView textDate = itemView.findViewById(R.id.textDate);
                TextView textWeight = itemView.findViewById(R.id.textWeight);
                View btnEdit = itemView.findViewById(R.id.btnEdit);
                View btnDelete = itemView.findViewById(R.id.btnDelete);

                textDate.setText(date);
                textWeight.setText(String.format(java.util.Locale.US, "%.1f", weight));

                // Handle delete action
                btnDelete.setOnClickListener(v -> {
                    dbHelper.deleteWeight(id);
                    loadWeights(); // Reload list after deletion
                });

                // Handle edit action by launching AddWeightActivity with existing data
                final double finalWeight = weight;
                btnEdit.setOnClickListener(v -> {
                    Intent intent = new Intent(WeightListActivity.this, AddWeightActivity.class);
                    intent.putExtra("entryId", id);
                    intent.putExtra("date", date);
                    intent.putExtra("weight", finalWeight);
                    startActivity(intent);
                });

                // Add the row to the container
                layoutWeightList.addView(itemView);

            } while (cursor.moveToNext());
        }
        cursor.close();
    }
}
