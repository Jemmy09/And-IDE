package com.AndIde.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.android.material.switchmaterial.SwitchMaterial;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private ImageView ivProfilePicture;
    private EditText etProfileName, etProfileBirthday, etProfileAge;
    private SwitchMaterial swDarkMode, swAiFileAccess, swAutoSave, swShowLineNumbers, swConfirmDelete;
    private RadioButton rbFontSmall, rbFontMedium, rbFontBig;
    private DatabaseHelper dbHelper;
    private String userEmail;
    private byte[] imageByteArray;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri selectedImage = result.getData().getData();
                    if (selectedImage != null) {
                        try (InputStream inputStream = getContentResolver().openInputStream(selectedImage)) {
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            if (bitmap != null) {
                                // Scale down bitmap to prevent TransactionTooLargeException or DB size issues
                                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
                                ivProfilePicture.setImageBitmap(scaledBitmap);

                                try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                                    imageByteArray = stream.toByteArray();
                                }
                            }
                        } catch (Exception e) {
                            android.util.Log.e("SettingsActivity", "Error loading image", e);
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_settings);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.settings_toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.settings_toolbar).setPadding(0, insets.top, 0, 0);
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });

        dbHelper = DatabaseHelper.getInstance(this);
        userEmail = getSharedPreferences("AndIde_Prefs", MODE_PRIVATE).getString("logged_in_email", null);

        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        etProfileName = findViewById(R.id.etProfileName);
        etProfileBirthday = findViewById(R.id.etProfileBirthday);
        etProfileAge = findViewById(R.id.etProfileAge);
        
        android.view.View btnChangePicture = findViewById(R.id.btnChangePicture);
        android.view.View btnSaveProfile = findViewById(R.id.btnSaveProfile);

        swDarkMode = findViewById(R.id.swDarkMode);
        swAutoSave = findViewById(R.id.swAutoSave);
        swShowLineNumbers = findViewById(R.id.swShowLineNumbers);
        swConfirmDelete = findViewById(R.id.swConfirmDelete);
        
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        swAiFileAccess = findViewById(R.id.swAiFileAccess);
        rbFontSmall = findViewById(R.id.rbFontSmall);
        rbFontMedium = findViewById(R.id.rbFontMedium);
        rbFontBig = findViewById(R.id.rbFontBig);

        loadUserData();
        loadThemePreference();
        loadFontSizePreference();

        etProfileBirthday.setFocusable(false);
        etProfileBirthday.setOnClickListener(v -> showDatePicker());

        btnChangePicture.setOnClickListener(v -> dispatchPickPictureIntent());

        btnSaveProfile.setOnClickListener(v -> saveUserData());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog dialog = new android.app.DatePickerDialog(this,
                (view, year1, month1, dayOfMonth) -> {
                    String date = year1 + "-" + String.format(Locale.US, "%02d", (month1 + 1)) + "-" + String.format(Locale.US, "%02d", dayOfMonth);
                    etProfileBirthday.setText(date);
                }, year, month, day);
        dialog.show();
    }

    private void loadUserData() {
        if (userEmail == null) return;
        try (Cursor cursor = dbHelper.getUser(userEmail)) {
            if (cursor != null && cursor.moveToFirst()) {
                etProfileName.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)));
                etProfileBirthday.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BIRTHDAY)));
                etProfileAge.setText(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AGE))));
                
                byte[] img = cursor.getBlob(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROFILE_IMAGE));
                if (img != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(img, 0, img.length);
                    ivProfilePicture.setImageBitmap(bitmap);
                    imageByteArray = img;
                }
            }
        } catch (Exception e) {
            android.util.Log.e("SettingsActivity", "Error loading user data", e);
        }
    }

    private void loadThemePreference() {
        SharedPreferences prefs = getSharedPreferences("AndIde_Prefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        swDarkMode.setChecked(isDarkMode);
        
        swAutoSave.setChecked(prefs.getBoolean("auto_save", true));
        swShowLineNumbers.setChecked(prefs.getBoolean("show_line_numbers", true));
        swConfirmDelete.setChecked(prefs.getBoolean("confirm_delete", true));
        
        boolean aiAccess = prefs.getBoolean("ai_file_access", false);
        swAiFileAccess.setChecked(aiAccess);
    }

    private void loadFontSizePreference() {
        SharedPreferences prefs = getSharedPreferences("AndIde_Prefs", MODE_PRIVATE);
        String fontSize = prefs.getString("editor_font_size", "medium");
        if (fontSize.equals("small")) rbFontSmall.setChecked(true);
        else if (fontSize.equals("big")) rbFontBig.setChecked(true);
        else rbFontMedium.setChecked(true);
    }

    private void saveUserData() {
        String name = etProfileName.getText().toString();
        String birthday = etProfileBirthday.getText().toString();
        int ageValue = 0;
        try {
            ageValue = Integer.parseInt(etProfileAge.getText().toString());
        } catch (NumberFormatException ignored) {}
        final int age = ageValue;

        // Save theme
        SharedPreferences.Editor editor = getSharedPreferences("AndIde_Prefs", MODE_PRIVATE).edit();
        editor.putBoolean("dark_mode", swDarkMode.isChecked());
        editor.putString("editor_theme", swDarkMode.isChecked() ? "dark" : "light");
        editor.putBoolean("auto_save", swAutoSave.isChecked());
        editor.putBoolean("show_line_numbers", swShowLineNumbers.isChecked());
        editor.putBoolean("confirm_delete", swConfirmDelete.isChecked());
        
        // Update current session theme immediately
        if (swDarkMode.isChecked()) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }
        editor.putBoolean("ai_file_access", swAiFileAccess.isChecked());

        // Save font size
        String selectedFontSize = "medium";
        if (rbFontSmall.isChecked()) selectedFontSize = "small";
        else if (rbFontBig.isChecked()) selectedFontSize = "big";
        editor.putString("editor_font_size", selectedFontSize);

        editor.apply();

        // Async DB Update
        dbExecutor.execute(() -> {
            boolean success = dbHelper.updateUserProfile(userEmail, name, birthday, age, imageByteArray);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(SettingsActivity.this, "Profile and Settings Updated", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(SettingsActivity.this, "Update Failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void dispatchPickPictureIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

}