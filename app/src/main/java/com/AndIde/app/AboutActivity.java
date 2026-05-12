package com.AndIde.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.widget.Toolbar;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AboutActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;
    private String currentLocation = "Not shared";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.about_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.toolbar).setPadding(0, insets.top, 0, 0);
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("About And-Ide");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        TextView tvAndroidVersion = findViewById(R.id.tvAndroidVersion);
        String deviceDetails = "Model: " + Build.MODEL + "\n" +
                "Brand: " + Build.BRAND + "\n" +
                "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
        tvAndroidVersion.setText(deviceDetails);

        Button btnSendFeedback = findViewById(R.id.btnSendFeedback);
        Button btnFacebook = findViewById(R.id.btnFacebook);

        btnSendFeedback.setOnClickListener(v -> requestLocationAndSendFeedback());
        btnFacebook.setOnClickListener(v -> openUrl("https://www.facebook.com/jemmy.francisco.73"));
    }

    private void requestLocationAndSendFeedback() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchLocationAndSend();
        }
    }

    private void fetchLocationAndSend() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            currentLocation = addresses.get(0).getAddressLine(0);
                        } else {
                            currentLocation = location.getLatitude() + ", " + location.getLongitude();
                        }
                    } catch (IOException e) {
                        currentLocation = location.getLatitude() + ", " + location.getLongitude();
                    }
                }
                sendFeedbackEmail();
            });
        } else {
            sendFeedbackEmail();
        }
    }

    private void sendFeedbackEmail() {
        String body = "\n\n--- Device Info ---\n" +
                "Model: " + Build.MODEL + "\n" +
                "Brand: " + Build.BRAND + "\n" +
                "Android Version: " + Build.VERSION.RELEASE + "\n" +
                "API Level: " + Build.VERSION.SDK_INT + "\n" +
                "Location: " + currentLocation + "\n" +
                "App Version: 1.1.3\n";

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + Uri.encode(getString(R.string.developer_email))));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
        intent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            startActivity(intent);
        } catch (Exception e) {
            // Fallback for devices without ACTION_SENDTO support
            Intent fallback = new Intent(Intent.ACTION_SEND);
            fallback.setType("message/rfc822");
            fallback.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.developer_email)});
            fallback.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
            fallback.putExtra(Intent.EXTRA_TEXT, body);
            try {
                startActivity(Intent.createChooser(fallback, "Send feedback via..."));
            } catch (Exception e2) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            fetchLocationAndSend();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}