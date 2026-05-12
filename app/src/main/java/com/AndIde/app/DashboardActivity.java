package com.AndIde.app;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.content.Intent;
import android.view.View;
import android.util.Log;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    private TextView tvTotalFiles, tvTotalLines, tvStorageUsed, tvPhpFiles;
    private RecyclerView rvRecentFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_dashboard);

        Toolbar toolbar = findViewById(R.id.dashboard_toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashboard_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.dashboard_toolbar).setPadding(0, insets.top, 0, 0);
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Project Dashboard");
        }

        tvTotalFiles = findViewById(R.id.tvTotalFiles);
        tvTotalLines = findViewById(R.id.tvTotalLines);
        tvStorageUsed = findViewById(R.id.tvStorageUsed);
        tvPhpFiles = findViewById(R.id.tvPhpFiles);
        rvRecentFiles = findViewById(R.id.rvRecentFiles);

        rvRecentFiles.setLayoutManager(new LinearLayoutManager(this));

        calculateStats();
    }

    private void calculateStats() {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            File dir = getFilesDir();
            File[] files = dir.listFiles();
            if (files == null) return;

            int totalFiles = 0;
            int totalLines = 0;
            long totalSize = 0;
            int phpCount = 0;

            List<File> fileList = new ArrayList<>();
            for (File f : files) {
                if (f.isFile()) fileList.add(f);
            }
            
            // Sort by last modified (recent first)
            fileList.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

            for (File file : fileList) {
                totalFiles++;
                totalSize += file.length();
                if (file.getName().endsWith(".php")) {
                    phpCount++;
                }
                totalLines += countLines(file);
            }

            final int fTotalFiles = totalFiles;
            final int fTotalLines = totalLines;
            final long fTotalSize = totalSize;
            final int fPhpCount = phpCount;
            final List<File> recentFiles = new ArrayList<>();
            for (int i = 0; i < Math.min(5, fileList.size()); i++) {
                recentFiles.add(fileList.get(i));
            }

            runOnUiThread(() -> {
                tvTotalFiles.setText(String.valueOf(fTotalFiles));
                tvTotalLines.setText(String.valueOf(fTotalLines));
                tvStorageUsed.setText(formatSize(fTotalSize));
                tvPhpFiles.setText(String.valueOf(fPhpCount));

                if (recentFiles.isEmpty()) {
                    rvRecentFiles.setVisibility(View.GONE);
                } else {
                    rvRecentFiles.setVisibility(View.VISIBLE);
                    FileAdapter fileAdapter = new FileAdapter(recentFiles, file -> {
                        Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
                        intent.putExtra("OPEN_FILE", file.getName());
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }, null);
                    rvRecentFiles.setAdapter(fileAdapter);
                }
            });
        });
        executor.shutdown();
    }

    private int countLines(File file) {
        int lines = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while (reader.readLine() != null) lines++;
        } catch (Exception e) {
            Log.e(TAG, "Error counting lines in " + file.getName(), e);
        }
        return lines;
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format(Locale.US, "%.1f %sB", (double)size / (1L << (z * 10)), " KMGTPE".charAt(z));
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}