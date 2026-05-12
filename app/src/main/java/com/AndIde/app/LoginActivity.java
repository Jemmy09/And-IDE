package com.AndIde.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleSignInResult(task);
                } else {
                    showLoading(false);
                    if (result.getResultCode() == RESULT_CANCELED) {
                        Toast.makeText(this, "Sign-In canceled by user.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private EditText etEmail, etPassword;
    private CheckBox cbRememberMe;
    private Button btnLogin, btnGoogleLogin;
    private TextView tvRegister;
    private ProgressBar progressBar;
    private DatabaseHelper dbHelper;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        mAuth = FirebaseAuth.getInstance();
        dbHelper = DatabaseHelper.getInstance(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        tvRegister = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progressBar);

        // Safely check for Google Play Services before initialization
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        
        if (resultCode == ConnectionResult.SUCCESS) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            btnGoogleLogin.setVisibility(View.VISIBLE);
        } else {
            // Hide Google login on non-GMS devices
            btnGoogleLogin.setVisibility(View.GONE);
            Log.w(TAG, "Google Play Services not available. GMS-dependent features disabled.");
        }

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter credentials", Toast.LENGTH_SHORT).show();
                return;
            }

            showLoading(true);
            final String finalPassword = password;
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            dbExecutor.execute(() -> {
                                if (!dbHelper.checkUserExists(email)) {
                                    dbHelper.registerUser(email, finalPassword, "User", "", 0);
                                }
                                runOnUiThread(() -> proceedToMain(email));
                            });
                        } else {
                            dbExecutor.execute(() -> {
                                if (dbHelper.checkUser(email, finalPassword)) {
                                    runOnUiThread(() -> proceedToMain(email));
                                } else {
                                    runOnUiThread(() -> {
                                        showLoading(false);
                                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                                        Toast.makeText(LoginActivity.this, "Authentication Failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });
                        }
                    });
        });

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        btnGoogleLogin.setOnClickListener(v -> {
            if (mGoogleSignInClient != null) {
                showLoading(true);
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            } else {
                Toast.makeText(this, "Google Login is not supported on this device.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnGoogleLogin.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }

    private void proceedToMain(String email) {
        boolean rememberMe = cbRememberMe.isChecked();
        android.content.SharedPreferences prefs = getSharedPreferences("AndIde_Prefs", MODE_PRIVATE);
        prefs.edit()
                .putBoolean("is_logged_in", rememberMe)
                .putString("logged_in_email", email)
                .apply();

        showLoading(false);
        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("USER_EMAIL", email);
        startActivity(intent);
        finish();
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account);
            } else {
                showLoading(false);
                Toast.makeText(this, "Google Sign-In account is null.", Toast.LENGTH_SHORT).show();
            }
        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            showLoading(false);
            String message = "Google Sign-In failed.";
            if (e.getStatusCode() == 12501) message = "Sign-In canceled by user.";
            else if (e.getStatusCode() == 7) message = "Network Error. Please check your connection.";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        String idToken = acct.getIdToken();
        if (idToken == null) {
            showLoading(false);
            Toast.makeText(this, "Could not get ID Token from Google", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String email = user.getEmail();
                            String name = user.getDisplayName();

                            if (email != null) {
                                dbExecutor.execute(() -> {
                                    if (!dbHelper.checkUserExists(email)) {
                                        dbHelper.registerUser(email, "google_login_no_password", name != null ? name : "User", "", 0);
                                    } else {
                                        try (Cursor cursor = dbHelper.getUser(email)) {
                                            if (cursor != null && cursor.moveToFirst()) {
                                                int bdayIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_BIRTHDAY);
                                                int ageIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_AGE);
                                                
                                                String existingBirthday = bdayIdx != -1 ? cursor.getString(bdayIdx) : "";
                                                int existingAge = ageIdx != -1 ? cursor.getInt(ageIdx) : 0;
                                                
                                                dbHelper.updateUserProfile(email, name != null ? name : "User", existingBirthday, existingAge, null);
                                            }
                                        }
                                    }
                                    runOnUiThread(() -> proceedToMain(email));
                                });
                            }
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        showLoading(false);
                        Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}