package net.azterisk.weighttracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "weight_tracker.db";
    private static final int DATABASE_VERSION = 1;

    // User Table
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "_id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";

    // Weight Table
    public static final String TABLE_WEIGHT = "weight_entries";
    public static final String COLUMN_WEIGHT_ID = "_id";
    public static final String COLUMN_WEIGHT_USER_ID = "user_id";
    public static final String COLUMN_WEIGHT_DATE = "date";
    public static final String COLUMN_WEIGHT_VALUE = "weight";

    // Table creation SQL
    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USERNAME + " TEXT UNIQUE, " +
                    COLUMN_PASSWORD + " TEXT);";

    private static final String CREATE_TABLE_WEIGHT =
            "CREATE TABLE " + TABLE_WEIGHT + " (" +
                    COLUMN_WEIGHT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_WEIGHT_USER_ID + " INTEGER, " +
                    COLUMN_WEIGHT_DATE + " TEXT, " +
                    COLUMN_WEIGHT_VALUE + " REAL, " +
                    "FOREIGN KEY(" + COLUMN_WEIGHT_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "));";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_WEIGHT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // --- User Operations ---

    /**
     * Checks if a user with the given username exists.
     * @param username the username to check.
     * @return true if the user exists, false otherwise.
     */
    public boolean checkUserExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_ID},
                COLUMN_USERNAME + "=?", new String[]{username},
                null, null, null);
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    /**
     * Authenticates a user.
     * @param username the username.
     * @param password the password.
     * @return true if authentication succeeds, false otherwise.
     */
    public boolean authenticateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_ID},
                COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?",
                new String[]{username, password},
                null, null, null);
        boolean success = (cursor.getCount() > 0);
        cursor.close();
        return success;
    }

    /**
     * Registers a new user.
     * @param username the new username.
     * @param password the new password.
     * @return true if inserted successfully, false otherwise.
     */
    public boolean registerUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    // Retrieve the user ID based on the username to tie weight entries to a specific user
    public long getUserId(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_ID},
                COLUMN_USERNAME + "=?", new String[]{username},
                null, null, null);
        long id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
        }
        cursor.close();
        return id;
    }

    // --- Weight Operations ---

    /**
     * Adds a new weight entry.
     * @param userId the ID of the user.
     * @param date the date as a string.
     * @param weight the weight value.
     * @return true if inserted successfully, false otherwise.
     */
    public boolean addWeight(long userId, String date, double weight) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_WEIGHT_USER_ID, userId);
        values.put(COLUMN_WEIGHT_DATE, date);
        values.put(COLUMN_WEIGHT_VALUE, weight);

        long result = db.insert(TABLE_WEIGHT, null, values);
        return result != -1;
    }

    /**
     * Updates an existing weight entry.
     * @param entryId the ID of the weight entry.
     * @param date the new date.
     * @param weight the new weight.
     * @return true if updated successfully, false otherwise.
     */
    public boolean updateWeight(long entryId, String date, double weight) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_WEIGHT_DATE, date);
        values.put(COLUMN_WEIGHT_VALUE, weight);

        int result = db.update(TABLE_WEIGHT, values, COLUMN_WEIGHT_ID + "=?",
                new String[]{String.valueOf(entryId)});
        return result > 0;
    }

    /**
     * Deletes a weight entry.
     * @param entryId the ID of the weight entry to delete.
     * @return true if deleted successfully, false otherwise.
     */
    public boolean deleteWeight(long entryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_WEIGHT, COLUMN_WEIGHT_ID + "=?",
                new String[]{String.valueOf(entryId)});
        return result > 0;
    }

    /**
     * Retrieves all weight entries for a specific user.
     * @param userId the ID of the user.
     * @return a Cursor containing the entries.
     */
    public Cursor getWeightsForUser(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_WEIGHT,
                null,
                COLUMN_WEIGHT_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, COLUMN_WEIGHT_DATE + " ASC"); // Sort by date or id
    }
}
