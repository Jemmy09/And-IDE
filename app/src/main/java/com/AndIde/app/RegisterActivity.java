package com.AndIde.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import android.text.Html;
import com.google.firebase.auth.FirebaseAuth;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private EditText etRegName, etRegEmail, etRegPassword;
    private CheckBox cbAgreeTerms;
    private TextView tvLogin, tvShowTerms;
    private Button btnRegister;
    private ProgressBar progressBar;
    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        dbHelper = DatabaseHelper.getInstance(this);
        mAuth = FirebaseAuth.getInstance();

        etRegName = findViewById(R.id.etRegName);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        cbAgreeTerms = findViewById(R.id.cbAgreeTerms);
        tvShowTerms = findViewById(R.id.tvShowTerms);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        tvShowTerms.setOnClickListener(v -> showTermsDialog());

        btnRegister.setOnClickListener(v -> {
            String name = etRegName.getText().toString().trim();
            String email = etRegEmail.getText().toString().trim();
            String password = etRegPassword.getText().toString();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(RegisterActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
            } else if (password.length() < 6) {
                Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            } else if (!cbAgreeTerms.isChecked()) {
                Toast.makeText(RegisterActivity.this, getString(R.string.error_must_agree_terms), Toast.LENGTH_SHORT).show();
            } else {
                setLoading(true);
                dbExecutor.execute(() -> {
                    boolean exists = dbHelper.checkUserExists(email);
                    runOnUiThread(() -> {
                        if (exists) {
                            setLoading(false);
                            Toast.makeText(RegisterActivity.this, "Email already registered", Toast.LENGTH_SHORT).show();
                        } else {
                            mAuth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(this, task -> {
                                        if (task.isSuccessful()) {
                                            dbExecutor.execute(() -> {
                                                boolean success = dbHelper.registerUser(name, email, password);
                                                runOnUiThread(() -> {
                                                    if (success) {
                                                        Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    } else {
                                                        setLoading(false);
                                                        Toast.makeText(RegisterActivity.this, "Local registration failed", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            });
                                        } else {
                                            setLoading(false);
                                            Toast.makeText(RegisterActivity.this, "Firebase Registration Failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    });
                });
            }
        });

        tvLogin.setOnClickListener(v -> finish());
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
        etRegName.setEnabled(!isLoading);
        etRegEmail.setEnabled(!isLoading);
        etRegPassword.setEnabled(!isLoading);
        cbAgreeTerms.setEnabled(!isLoading);
        tvLogin.setEnabled(!isLoading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }

    private void showTermsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.terms_and_conditions_title)
                .setMessage(Html.fromHtml(getString(R.string.terms_and_conditions_content), Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("OK", null)
                .show();
    }
}