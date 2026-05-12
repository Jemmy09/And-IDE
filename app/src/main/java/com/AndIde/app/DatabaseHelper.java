package com.AndIde.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper instance;
    private static final String DATABASE_NAME = "AndIde.db";
    private static final int DATABASE_VERSION = 4;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static final String TABLE_USERS = "users";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_BIRTHDAY = "birthday";
    public static final String COLUMN_AGE = "age";
    public static final String COLUMN_PROFILE_IMAGE = "profile_image";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_EMAIL + " TEXT UNIQUE, " +
                    COLUMN_PASSWORD + " TEXT, " +
                    COLUMN_BIRTHDAY + " TEXT, " +
                    COLUMN_AGE + " INTEGER, " +
                    COLUMN_PROFILE_IMAGE + " BLOB" +
                    ");";

    public static final String TABLE_HISTORY = "code_history";
    public static final String COLUMN_HISTORY_ID = "id";
    public static final String COLUMN_FILE_NAME = "file_name";
    public static final String COLUMN_CODE_CONTENT = "code_content";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    public static final String TABLE_SNIPPETS = "snippets";
    public static final String COLUMN_SNIPPET_ID = "id";
    public static final String COLUMN_SNIPPET_TITLE = "title";
    public static final String COLUMN_SNIPPET_CODE = "code";
    public static final String COLUMN_SNIPPET_LANGUAGE = "language";

    public static final String TABLE_CHAT_HISTORY = "chat_history";
    public static final String COLUMN_CHAT_ID = "id";
    public static final String COLUMN_CHAT_USER_EMAIL = "user_email";
    public static final String COLUMN_CHAT_MESSAGE = "message";
    public static final String COLUMN_CHAT_IS_USER = "is_user";
    public static final String COLUMN_CHAT_TIMESTAMP = "timestamp";

    private static final String HISTORY_TABLE_CREATE =
            "CREATE TABLE " + TABLE_HISTORY + " (" +
                    COLUMN_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_FILE_NAME + " TEXT, " +
                    COLUMN_CODE_CONTENT + " TEXT, " +
                    COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");";

    private static final String SNIPPETS_TABLE_CREATE =
            "CREATE TABLE " + TABLE_SNIPPETS + " (" +
                    COLUMN_SNIPPET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_SNIPPET_TITLE + " TEXT, " +
                    COLUMN_SNIPPET_CODE + " TEXT, " +
                    COLUMN_SNIPPET_LANGUAGE + " TEXT" +
                    ");";

    private static final String CHAT_HISTORY_TABLE_CREATE =
            "CREATE TABLE " + TABLE_CHAT_HISTORY + " (" +
                    COLUMN_CHAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CHAT_USER_EMAIL + " TEXT, " +
                    COLUMN_CHAT_MESSAGE + " TEXT, " +
                    COLUMN_CHAT_IS_USER + " INTEGER, " +
                    COLUMN_CHAT_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");";


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        db.execSQL(HISTORY_TABLE_CREATE);
        db.execSQL(SNIPPETS_TABLE_CREATE);
        db.execSQL(CHAT_HISTORY_TABLE_CREATE);
        
        // Add some default snippets
        db.execSQL("INSERT INTO " + TABLE_SNIPPETS + " (" + COLUMN_SNIPPET_TITLE + ", " + COLUMN_SNIPPET_CODE + ", " + COLUMN_SNIPPET_LANGUAGE + ") VALUES ('PHP Boilerplate', '<?php\n\n?>', 'PHP')");
        db.execSQL("INSERT INTO " + TABLE_SNIPPETS + " (" + COLUMN_SNIPPET_TITLE + ", " + COLUMN_SNIPPET_CODE + ", " + COLUMN_SNIPPET_LANGUAGE + ") VALUES ('HTML5 Skeleton', '<!DOCTYPE html>\n<html>\n<head>\n<title></title>\n</head>\n<body>\n\n</body>\n</html>', 'HTML')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(HISTORY_TABLE_CREATE);
        }
        if (oldVersion < 3) {
            db.execSQL(SNIPPETS_TABLE_CREATE);
            db.execSQL("INSERT INTO " + TABLE_SNIPPETS + " (" + COLUMN_SNIPPET_TITLE + ", " + COLUMN_SNIPPET_CODE + ", " + COLUMN_SNIPPET_LANGUAGE + ") VALUES ('PHP Boilerplate', '<?php\n\n?>', 'PHP')");
            db.execSQL("INSERT INTO " + TABLE_SNIPPETS + " (" + COLUMN_SNIPPET_TITLE + ", " + COLUMN_SNIPPET_CODE + ", " + COLUMN_SNIPPET_LANGUAGE + ") VALUES ('HTML5 Skeleton', '<!DOCTYPE html>\n<html>\n<head>\n<title></title>\n</head>\n<body>\n\n</body>\n</html>', 'HTML')");
        }
        if (oldVersion < 4) {
            db.execSQL(CHAT_HISTORY_TABLE_CREATE);
        }
    }

    public boolean registerUser(String email, String password, String name, String birthday, int age) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD, password);
        values.put(COLUMN_BIRTHDAY, birthday);
        values.put(COLUMN_AGE, age);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean checkUserExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_ID}, COLUMN_EMAIL + " = ?", new String[]{email}, null, null, null);
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    public boolean registerUser(String name, String email, String password) {
        return registerUser(email, password, name, "", 0);
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_ID};
        String selection = COLUMN_EMAIL + " = ?" + " AND " + COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {email, password};
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    public Cursor getUser(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_USERS, null, COLUMN_EMAIL + " = ?", new String[]{email}, null, null, null);
    }

    public boolean updateUserProfile(String email, String name, String birthday, int age, byte[] image) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_BIRTHDAY, birthday);
        values.put(COLUMN_AGE, age);
        if (image != null) {
            values.put(COLUMN_PROFILE_IMAGE, image);
        }
        int result = db.update(TABLE_USERS, values, COLUMN_EMAIL + " = ?", new String[]{email});
        return result > 0;
    }

    public boolean deleteUser(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete user record
        int result = db.delete(TABLE_USERS, COLUMN_EMAIL + " = ?", new String[]{email});
        
        // Also delete their chat history to keep DB clean
        db.delete(TABLE_CHAT_HISTORY, COLUMN_CHAT_USER_EMAIL + " = ?", new String[]{email});

        return result > 0;
    }
}