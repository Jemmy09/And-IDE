package com.AndIde.app;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class AiPreviewActivity extends AppCompatActivity {

    private EditText etOriginalCode, etProposedCode;
    private TextView tvTargetFile;
    private Button btnApply, btnCancel;
    private String targetFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_ai_preview);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.ai_preview_toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ai_preview_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.ai_preview_toolbar).setPadding(0, insets.top, 0, 0);
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });
        setSupportActionBar(toolbar);

        etOriginalCode = findViewById(R.id.etOriginalCode);
        etProposedCode = findViewById(R.id.etProposedCode);
        tvTargetFile = findViewById(R.id.tvTargetFile);
        btnApply = findViewById(R.id.btnApply);
        btnCancel = findViewById(R.id.btnCancel);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String proposedCode = getIntent().getStringExtra("PROPOSED_CODE");
        targetFileName = getIntent().getStringExtra("TARGET_FILE");

        tvTargetFile.setText("Target File: " + targetFileName);
        etProposedCode.setText(proposedCode);

        loadOriginalFile();

        btnCancel.setOnClickListener(v -> finish());

        btnApply.setOnClickListener(v -> {
            saveChanges();
        });
    }

    private void loadOriginalFile() {
        if (targetFileName == null || targetFileName.contains("/") || targetFileName.contains("\\")) {
            Toast.makeText(this, "Invalid file path detected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        try {
            FileInputStream fis = openFileInput(targetFileName);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String text;
            while ((text = br.readLine()) != null) {
                sb.append(text).append("\n");
            }
            etOriginalCode.setText(sb.toString());
        } catch (Exception e) {
            android.util.Log.e("AiPreview", "Error loading original file: " + targetFileName, e);
            etOriginalCode.setText("// File not found or empty");
        }
    }

    private void saveChanges() {
        String newCode = etProposedCode.getText().toString();
        try {
            FileOutputStream fos = openFileOutput(targetFileName, MODE_PRIVATE);
            fos.write(newCode.getBytes());
            fos.close();
            Toast.makeText(this, "Changes applied to " + targetFileName, Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            android.util.Log.e("AiPreview", "Error applying changes to: " + targetFileName, e);
            Toast.makeText(this, "Failed to apply changes", Toast.LENGTH_SHORT).show();
        }
    }
}