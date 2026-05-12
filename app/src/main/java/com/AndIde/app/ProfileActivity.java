package com.AndIde.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private ImageView ivProfilePicture;
    private EditText etProfileName, etProfileBirthday, etProfileAge, etProfileLocation;
    private Button btnChangePicture, btnSaveProfile, btnGetLocation, btnDeleteAccount;
    private DatabaseHelper dbHelper;
    private String userEmail;
    private byte[] imageByteArray;
    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri selectedImage = result.getData().getData();
                    if (selectedImage != null) {
                        try {
                            java.io.InputStream inputStream = getContentResolver().openInputStream(selectedImage);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                            // Scale down bitmap to prevent TransactionTooLargeException or DB size issues
                            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
                            ivProfilePicture.setImageBitmap(scaledBitmap);

                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                            imageByteArray = stream.toByteArray();
                        } catch (Exception e) {
                            android.util.Log.e("ProfileActivity", "Error loading image", e);
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
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.profile_toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.profile_toolbar).setPadding(0, insets.top, 0, 0);
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });
        setSupportActionBar(toolbar);
        dbHelper = DatabaseHelper.getInstance(this);
        userEmail = getSharedPreferences("AndIde_Prefs", MODE_PRIVATE).getString("logged_in_email", null);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        etProfileName = findViewById(R.id.etProfileName);
        etProfileBirthday = findViewById(R.id.etProfileBirthday);
        etProfileAge = findViewById(R.id.etProfileAge);
        etProfileLocation = findViewById(R.id.etProfileLocation);
        btnChangePicture = findViewById(R.id.btnChangePicture);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Developer Profile");
        }

        loadUserData();

        etProfileBirthday.setFocusable(false);
        etProfileBirthday.setOnClickListener(v -> showDatePicker());

        btnChangePicture.setOnClickListener(v -> dispatchPickPictureIntent());

        btnSaveProfile.setOnClickListener(v -> saveUserData());

        btnGetLocation.setOnClickListener(v -> checkLocationPermission());

        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account_dialog_title)
                .setMessage(R.string.delete_account_dialog_message)
                .setPositiveButton(R.string.btn_confirm_delete, (dialog, which) -> deleteAccount())
                .setNegativeButton("CANCEL", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Delete from Firebase Auth
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Delete from Local DB
                    dbHelper.deleteUser(userEmail);
                    
                    // Clear Preferences
                    getSharedPreferences("AndIde_Prefs", MODE_PRIVATE).edit().clear().apply();
                    
                    Toast.makeText(this, R.string.account_deleted_success, Toast.LENGTH_LONG).show();
                    
                    // Go to Login
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, getString(R.string.account_deleted_error), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLastLocation();
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        String address = addresses.get(0).getAddressLine(0);
                        etProfileLocation.setText(address);
                    } else {
                        etProfileLocation.setText(location.getLatitude() + ", " + location.getLongitude());
                    }
                } catch (IOException e) {
                    etProfileLocation.setText(location.getLatitude() + ", " + location.getLongitude());
                }
            } else {
                Toast.makeText(this, "Unable to get location. Make sure GPS is on.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDatePicker() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int year = cal.get(java.util.Calendar.YEAR);
        int month = cal.get(java.util.Calendar.MONTH);
        int day = cal.get(java.util.Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog dialog = new android.app.DatePickerDialog(this,
                (view, year1, month1, dayOfMonth) -> {
                    String date = year1 + "-" + String.format(java.util.Locale.US, "%02d", (month1 + 1)) + "-" + String.format(java.util.Locale.US, "%02d", dayOfMonth);
                    etProfileBirthday.setText(date);
                }, year, month, day);
        dialog.show();
    }

    private void loadUserData() {
        if (userEmail == null) return;
        Cursor cursor = dbHelper.getUser(userEmail);
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
            cursor.close();
        }
    }

    private void saveUserData() {
        String name = etProfileName.getText().toString();
        String birthday = etProfileBirthday.getText().toString();
        int tempAge = 0;
        try {
            tempAge = Integer.parseInt(etProfileAge.getText().toString());
        } catch (NumberFormatException ignored) {}
        final int age = tempAge;

        // Async DB Update for consistency
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            boolean success = dbHelper.updateUserProfile(userEmail, name, birthday, age, imageByteArray);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(ProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ProfileActivity.this, "Update Failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
        executor.shutdown();
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    private void dispatchPickPictureIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

}