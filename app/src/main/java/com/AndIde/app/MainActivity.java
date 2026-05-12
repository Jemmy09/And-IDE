package com.AndIde.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import de.hdodenhof.circleimageview.CircleImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private EditText etCode;
    private TextView tvLineNumbers, tvDebugInfo, tvCurrentFile;
    private String currentFileName = "index.php";
    private final Handler autoSaveHandler = new Handler(android.os.Looper.getMainLooper());
    private Runnable autoSaveRunnable;
    private final Handler previewHandler = new Handler(android.os.Looper.getMainLooper());
    private Runnable previewRunnable;
    private final ExecutorService fileExecutor = Executors.newSingleThreadExecutor();
    private String userEmail;
    private boolean isHighlighting = false;
    private boolean isAiEnabled = false;
    private boolean isAutoSaveEnabled = false;
    private boolean isDirty = false;
    private boolean isInternalChange = false;

    private LinearLayout llSnippets, llTabs, llSearchBar, llLivePreview;
    private WebView wvPreview;
    private EditText etSearch, etReplace;
    private LinearLayout llCustomMenu;
    private FileAdapter fileAdapter;
    private List<File> fileList;
    private final List<String> openFiles = new ArrayList<>();
    private int lastSearchIndex = 0;
    private DatabaseHelper dbHelper;
    private CircleImageView ivNavProfile;
    private TextView tvNavName, tvNavEmail;

    private final ActivityResultLauncher<Intent> templateLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String code = data.getStringExtra("CODE");
                    String fileName = data.getStringExtra("FILENAME");
                    if (code != null) {
                        if (etCode != null) etCode.setText(code);
                        if (fileName != null) {
                            currentFileName = fileName;
                            if (tvCurrentFile != null) tvCurrentFile.setText(fileName);
                            addTab(fileName);
                        }
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> importFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri uri = result.getData().getData();
                    if (uri != null && fileExecutor != null) {
                        fileExecutor.execute(() -> {
                            try (java.io.InputStream is = getContentResolver().openInputStream(uri);
                                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                                StringBuilder sb = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                                String displayName = "imported.php";
                                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                                if (cursor != null && cursor.moveToFirst()) {
                                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                                    if (idx != -1) displayName = cursor.getString(idx);
                                    cursor.close();
                                }
                                final String finalName = displayName;
                                final String finalContent = sb.toString();
                                runOnUiThread(() -> {
                                    saveFile(finalName, finalContent, false);
                                    switchTab(finalName);
                                    addTab(finalName);
                                });
                            } catch (Exception e) {
                                android.util.Log.e("MainActivity", "Import failed", e);
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Import failed", Toast.LENGTH_SHORT).show());
                            }
                        });
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> saveAsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri uri = result.getData().getData();
                    if (uri != null && etCode != null && fileExecutor != null) {
                        final String content = etCode.getText().toString();
                        fileExecutor.execute(() -> {
                            try (java.io.OutputStream os = getContentResolver().openOutputStream(uri)) {
                                if (os != null) {
                                    os.write(content.getBytes());
                                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "File saved successfully", Toast.LENGTH_SHORT).show());
                                }
                            } catch (Exception e) {
                                android.util.Log.e("MainActivity", "Save As failed", e);
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Save As failed", Toast.LENGTH_SHORT).show());
                            }
                        });
                    }
                }
            }
    );


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String openFile = intent.getStringExtra("OPEN_FILE");
        int lineNumber = intent.getIntExtra("LINE_NUMBER", -1);
        
        if (openFile != null) {
            currentFileName = openFile;
            tvCurrentFile.setText(currentFileName);
            loadFile(currentFileName, lineNumber);
            addTab(currentFileName);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPreferences();
        updateNavHeader();
        // Re-apply highlighting to ensure colors match the current theme
        applyHighlighting(etCode.getText());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        View mainContent = findViewById(R.id.main_content);
        View bottomBar = findViewById(R.id.bottom_bar);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply top padding to main content to clear status bar
            mainContent.setPadding(0, insets.top, 0, 0);
            // Apply bottom padding to the horizontal bottom bar to clear navigation bar
            bottomBar.setPadding(0, 0, 0, insets.bottom);
            
            // Also apply padding to the drawer container to prevent overlap
            View navContainer = findViewById(R.id.nav_view_container);
            navContainer.setPadding(0, insets.top, 0, insets.bottom);

            // Lateral padding for cutout areas
            v.setPadding(insets.left, 0, insets.right, 0);
            return WindowInsetsCompat.CONSUMED;
        });

        userEmail = getIntent().getStringExtra("USER_EMAIL");
        if (userEmail == null) {
            userEmail = getSharedPreferences("AndIde_Prefs", MODE_PRIVATE).getString("logged_in_email", null);
        }
        dbHelper = DatabaseHelper.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        setupCustomDrawer();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        etCode = findViewById(R.id.etCode);
        tvLineNumbers = findViewById(R.id.tvLineNumbers);
        tvDebugInfo = findViewById(R.id.tvDebugInfo);
        tvCurrentFile = findViewById(R.id.tvCurrentFile);
        View btnSave = findViewById(R.id.btnSave);
        View btnOpen = findViewById(R.id.btnOpen);
        Button btnRun = findViewById(R.id.btnRun);
        Button btnDebug = findViewById(R.id.btnDebug);
        Button btnFormat = findViewById(R.id.btnFormat);
        Button btnHistory = findViewById(R.id.btnHistory);
        Button btnSnippets = findViewById(R.id.btnSnippets);
        View btnShare = findViewById(R.id.btnShare);
        llSnippets = findViewById(R.id.llSnippets);
        llTabs = findViewById(R.id.llTabs);
        llSearchBar = findViewById(R.id.llSearchBar);
        etSearch = findViewById(R.id.etSearch);
        etReplace = findViewById(R.id.etReplace);
        View btnFindNext = findViewById(R.id.btnFindNext);
        Button btnReplaceAll = findViewById(R.id.btnReplaceAll);
        View btnCloseSearch = findViewById(R.id.btnCloseSearch);
        llLivePreview = findViewById(R.id.llLivePreview);
        wvPreview = findViewById(R.id.wvPreview);

        wvPreview.getSettings().setJavaScriptEnabled(true);
        wvPreview.getSettings().setDomStorageEnabled(true);
        
        // Correctly initialize the JS Interface for Database connectivity
        wvPreview.addJavascriptInterface(new WebAppInterface(this), "AndroidDB");

        // Exit Safety: Warn on unsaved changes when exiting the app
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (llSearchBar.getVisibility() == View.VISIBLE) {
                    llSearchBar.setVisibility(View.GONE);
                } else if (llLivePreview.getVisibility() == View.VISIBLE) {
                    llLivePreview.setVisibility(View.GONE);
                } else if (isDirty && !isAutoSaveEnabled) {
                    new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                            .setTitle("Unsaved Changes")
                            .setMessage("You have unsaved changes in " + currentFileName + ". Do you want to save before exiting?")
                            .setPositiveButton("Save & Exit", (dialog, which) -> {
                                saveFile(currentFileName, etCode.getText().toString(), true);
                                finish();
                            })
                            .setNegativeButton("Discard", (dialog, which) -> finish())
                            .setNeutralButton("Cancel", null)
                            .show();
                } else {
                    finish();
                }
            }
        });

        tvCurrentFile.setText(currentFileName);
        
        btnOpen.setOnClickListener(v -> {
            if (llSearchBar.getVisibility() == View.VISIBLE) {
                llSearchBar.setVisibility(View.GONE);
            } else {
                llSearchBar.setVisibility(View.VISIBLE);
                etSearch.requestFocus();
            }
        });
        btnSave.setOnClickListener(v -> saveFile(currentFileName, etCode.getText().toString(), true));
        btnRun.setOnClickListener(v -> toggleLivePreview());
        btnDebug.setOnClickListener(v -> performDebug());
        btnFormat.setOnClickListener(v -> formatCode());
        btnHistory.setOnClickListener(v -> showCodeHistory());
        btnSnippets.setOnClickListener(v -> showSnippets());
        btnShare.setOnClickListener(v -> shareCode());

        btnFindNext.setOnClickListener(v -> findNext());
        btnReplaceAll.setOnClickListener(v -> replaceAll());
        btnCloseSearch.setOnClickListener(v -> llSearchBar.setVisibility(View.GONE));

        loadPreferences();
        setupCustomDrawer();
        setupSnippets();
        addTab(currentFileName);

        // Handle opening a specific file from Dashboard
        String openFile = getIntent().getStringExtra("OPEN_FILE");
        if (openFile != null) {
            currentFileName = openFile;
            tvCurrentFile.setText(currentFileName);
            loadFile(currentFileName, -1);
            addTab(currentFileName);
        }

        etCode.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            tvLineNumbers.scrollTo(0, scrollY);
        });

        etCode.addTextChangedListener(new TextWatcher() {
            private boolean isDeleting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                isDeleting = count > after;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isInternalChange) return;

                // Auto-closing brackets/quotes
                if (!isDeleting) {
                    int pos = etCode.getSelectionStart();
                    if (pos > 0) {
                        char lastChar = s.charAt(pos - 1);
                        String closeChar = null;
                        if (lastChar == '{') closeChar = "}";
                        else if (lastChar == '(') closeChar = ")";
                        else if (lastChar == '[') closeChar = "]";
                        else if (lastChar == '"') closeChar = "\"";
                        else if (lastChar == '\'') closeChar = "'";

                        if (closeChar != null) {
                            isInternalChange = true;
                            s.insert(pos, closeChar);
                            etCode.setSelection(pos);
                            isInternalChange = false;
                        }
                    }
                }

                isDirty = true;
                updateLineNumbers();
                if (!isHighlighting) {
                    applyHighlighting(s);
                }

                // Debounce for Live Preview
                previewHandler.removeCallbacks(previewRunnable);
                previewRunnable = () -> updateLivePreview();
                previewHandler.postDelayed(previewRunnable, 500); // Wait 500ms after last keystroke

                // Auto-Save Implementation
                if (isAutoSaveEnabled && !currentFileName.isEmpty()) {
                    autoSaveHandler.removeCallbacks(autoSaveRunnable);
                    autoSaveRunnable = () -> {
                        saveFile(currentFileName, s.toString(), false);
                        isDirty = false;
                    };
                    autoSaveHandler.postDelayed(autoSaveRunnable, 2000); // Save 2 seconds after last type
                }
            }
        });

        // Add "Explain Code" to context menu
        etCode.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                menu.add(0, 999, 0, "Explain Code").setIcon(android.R.drawable.ic_menu_help);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
                if (item.getItemId() == 999) {
                    int start = etCode.getSelectionStart();
                    int end = etCode.getSelectionEnd();
                    String selectedText = etCode.getText().toString().substring(start, end);
                    if (!selectedText.trim().isEmpty()) {
                        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                        intent.putExtra("USER_EMAIL", userEmail);
                        intent.putExtra("EXPLAIN_CODE", selectedText);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "Please select code to explain", Toast.LENGTH_SHORT).show();
                    }
                    mode.finish();
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        previewHandler.removeCallbacks(previewRunnable);
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        fileExecutor.shutdown();
    }

    private void showOpenFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        try {
            importFileLauncher.launch(intent);
        } catch (Exception ex) {
            Toast.makeText(this, "File manager not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportProjectAsZip() {
        fileExecutor.execute(() -> {
            File dir = getFilesDir();
            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "No files to export", Toast.LENGTH_SHORT).show());
                return;
            }

            // Use internal cache directory which is always accessible to the app and configured in FileProvider
            File zipFile = new File(getCacheDir(), "AndIde_Project.zip");
            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
                for (File file : files) {
                    if (file.isFile()) {
                        ZipEntry entry = new ZipEntry(file.getName());
                        zos.putNextEntry(entry);
                        try (FileInputStream fis = new FileInputStream(file)) {
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = fis.read(buffer)) > 0) {
                                zos.write(buffer, 0, len);
                            }
                        }
                        zos.closeEntry();
                    }
                }
                runOnUiThread(() -> {
                    try {
                        android.net.Uri contentUri = androidx.core.content.FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", zipFile);
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("application/zip");
                        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(intent, "Export Project Zip"));
                    } catch (Exception e) {
                        android.util.Log.e("Export", "Error sharing zip", e);
                        Toast.makeText(MainActivity.this, "Error sharing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("Export", "Zip creation failed", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void toggleLivePreview() {
        if (llLivePreview.getVisibility() == View.VISIBLE) {
            llLivePreview.setVisibility(View.GONE);
        } else {
            llLivePreview.setVisibility(View.VISIBLE);
            updateLivePreview();
        }
    }

    private void updateLivePreview() {
        if (llLivePreview.getVisibility() == View.VISIBLE) {
            String code = etCode.getText().toString();
            String fileName = currentFileName;
            fileExecutor.execute(() -> {
                String html;
                if (fileName.endsWith(".php") || code.contains("<?php")) {
                    html = getPhpRunnerHtml(code);
                } else if (fileName.endsWith(".html")) {
                    html = getCombinedHtml(code);
                } else {
                    html = code;
                }
                runOnUiThread(() -> wvPreview.loadDataWithBaseURL("https://and-ide.local/", html, "text/html", "UTF-8", null));
            });
        }
    }

    private String getCombinedHtml(String htmlCode) {
        String css = "";
        String js = "";
        
        File dir = getFilesDir();
        File cssFile = new File(dir, "style.css");
        if (cssFile.exists()) css = readFileContent(cssFile);
        
        File jsFile = new File(dir, "script.js");
        if (jsFile.exists()) js = readFileContent(jsFile);

        int bgColor = ContextCompat.getColor(this, R.color.pro_surface);
        int textColor = ContextCompat.getColor(this, R.color.pro_on_surface);
        String hexBg = String.format("#%06X", (0xFFFFFF & bgColor));
        String hexText = String.format("#%06X", (0xFFFFFF & textColor));

        return "<html><head><style>body{background-color:" + hexBg + ";color:" + hexText + ";}" + css + "</style></head><body>" + htmlCode + "<script>" + js + "</script></body></html>";
    }

    private String getPhpRunnerHtml(String code) {
        int bgColor = ContextCompat.getColor(this, R.color.pro_surface);
        int textColor = ContextCompat.getColor(this, R.color.pro_on_surface);
        int accentColor = ContextCompat.getColor(this, R.color.pro_accent);
        int errorBg = ContextCompat.getColor(this, R.color.debug_error_bg);
        int errorText = ContextCompat.getColor(this, R.color.debug_error_text);

        String hexBg = String.format("#%06X", (0xFFFFFF & bgColor));
        String hexText = String.format("#%06X", (0xFFFFFF & textColor));
        String hexAccent = String.format("#%06X", (0xFFFFFF & accentColor));
        String hexErrBg = String.format("#%06X", (0xFFFFFF & errorBg));
        String hexErrText = String.format("#%06X", (0xFFFFFF & errorText));

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "   <meta charset=\"UTF-8\">\n" +
                "   <style>\n" +
                "       body { font-family: 'Consolas', 'Monaco', monospace; padding: 15px; background: " + hexBg + "; color: " + hexText + "; line-height: 1.5; font-size: 11px; }\n" +
                "       .header { color: " + hexAccent + "; border-bottom: 1px solid rgba(128,128,128,0.3); padding-bottom: 4px; margin-bottom: 10px; font-size: 10px; }\n" +
                "       pre { white-space: pre-wrap; word-wrap: break-word; }\n" +
                "       .error { color: " + hexErrText + "; background: " + hexErrBg + "; padding: 8px; border-radius: 4px; }\n" +
                "       .db-log { color: " + hexAccent + "; font-size: 10px; margin-top: 10px; border-top: 1px dashed rgba(128,128,128,0.3); padding-top: 5px; opacity: 0.8; }\n" +
                "   </style>\n" +
                "   <script src=\"https://cdn.jsdelivr.net/npm/@php-wasm/web@0.0.9/dist/index.iife.js\"></script>\n" +
                "</head>\n" +
                "<body>\n" +
                "   <div class=\"header\">PHP Split View Mode (Live)</div>\n" +
                "   <div id=\"output\">Initializing Engine...</div>\n" +
                "   <div id=\"db-status\" class=\"db-log\">Database: Ready</div>\n" +
                "   <script>\n" +
                "       async function run() {\n" +
                "           const out = document.getElementById('output');\n" +
                "           try {\n" +
                "               const php = await PHP.loadPHP();\n" +
                "               \n" +
                "               php.registerFunction('exec_sql', (sql) => {\n" +
                "                   return window.AndroidDB ? AndroidDB.executeSql(sql) : '[]';\n" +
                "               });\n" +
                "\n" +
                "               let buffer = '';\n" +
                "               php.onMessage((msg) => { buffer += msg; });\n" +
                "               \n" +
                "               const userCode = `" + code.replace("`", "\\`").replace("\\", "\\\\").replace("$", "\\$") + "`;\n" +
                "               \n" +
                "               const fullCode = `<?php\n" +
                "               function mysqli_query($conn, $sql) { return json_decode(exec_sql($sql), true); }\n" +
                "               function mysqli_fetch_assoc($result) { return array_shift($result); }\n" +
                "               function mysqli_connect($h,$u,$p,$d) { return \"connected\"; }\n" +
                "               ?>` + userCode;\n" +
                "               \n" +
                "               await php.run(fullCode);\n" +
                "               out.innerHTML = '<pre>' + (buffer || '<i>(No output)</i>') + '</pre>';\n" +
                "           } catch (e) {\n" +
                "               out.innerHTML = '<div class=\"error\">' + e.message + '</div>';\n" +
                "           }\n" +
                "       }\n" +
                "       run();\n" +
                "   </script>\n" +
                "</body>\n" +
                "</html>";
    }

    private String readFileContent(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void findNext() {
        String query = etSearch.getText().toString();
        if (query.isEmpty()) return;

        String code = etCode.getText().toString();
        int index = code.indexOf(query, lastSearchIndex);
        if (index == -1) {
            index = code.indexOf(query); // Wrap around
        }

        if (index != -1) {
            etCode.setSelection(index, index + query.length());
            etCode.requestFocus();
            lastSearchIndex = index + query.length();
        } else {
            Toast.makeText(this, "Not found", Toast.LENGTH_SHORT).show();
            lastSearchIndex = 0;
        }
    }

    private void replaceAll() {
        String query = etSearch.getText().toString();
        String replacement = etReplace.getText().toString();
        if (query.isEmpty()) return;

        String code = etCode.getText().toString();
        String newCode = code.replace(query, replacement);
        
        isInternalChange = true;
        etCode.setText(newCode);
        isInternalChange = false;
        
        isDirty = true;
        Toast.makeText(this, "All occurrences replaced", Toast.LENGTH_SHORT).show();
    }

    private void saveFile(String fileName, String content, boolean showToast) {
        fileExecutor.execute(() -> {
            boolean success = false;
            try (FileOutputStream fos = openFileOutput(fileName, Context.MODE_PRIVATE)) {
                fos.write(content.getBytes());
                saveToHistory(fileName, content);
                success = true;
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Failed to save file: " + fileName, e);
            }

            final boolean finalSuccess = success;
            runOnUiThread(() -> {
                if (finalSuccess) {
                    isDirty = false;
                    if (showToast) Toast.makeText(MainActivity.this, "Saved: " + fileName, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Save failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void saveFileAs(String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        saveAsLauncher.launch(intent);
    }

    private void saveToHistory(String fileName, String content) {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(DatabaseHelper.COLUMN_FILE_NAME, fileName);
        values.put(DatabaseHelper.COLUMN_CODE_CONTENT, content);
        dbHelper.getWritableDatabase().insert(DatabaseHelper.TABLE_HISTORY, null, values);
    }

    private void addTab(String fileName) {
        if (!openFiles.contains(fileName)) {
            openFiles.add(fileName);
            View tabView = LayoutInflater.from(this).inflate(R.layout.tab_item, llTabs, false);
            TextView tvTabName = tabView.findViewById(R.id.tvTabName);
            ImageButton btnTabClose = tabView.findViewById(R.id.btnTabClose);

            tvTabName.setText(fileName);
            tabView.setTag(fileName);
            
            tabView.setOnClickListener(v -> switchTab((String) v.getTag()));
            btnTabClose.setOnClickListener(v -> {
                if (isDirty && !isAutoSaveEnabled && fileName.equals(currentFileName)) {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Unsaved Changes")
                            .setMessage("Do you want to save " + fileName + " before closing?")
                            .setPositiveButton("Save", (dialog, which) -> {
                                saveFile(fileName, etCode.getText().toString(), true);
                                closeTab(fileName, tabView);
                            })
                            .setNegativeButton("Discard", (dialog, which) -> {
                                isDirty = false; // Prevent switchTab from saving discarded changes
                                closeTab(fileName, tabView);
                            })
                            .setNeutralButton("Cancel", null)
                            .show();
                } else {
                    closeTab(fileName, tabView);
                }
            });

            llTabs.addView(tabView);
        }
        switchTab(fileName);
    }

    private void closeTab(String fileName, View tabView) {
        int index = openFiles.indexOf(fileName);
        openFiles.remove(fileName);
        llTabs.removeView(tabView);
        
        if (fileName.equals(currentFileName)) {
            if (!openFiles.isEmpty()) {
                // Switch to adjacent tab for better UX
                int nextIndex = Math.min(index, openFiles.size() - 1);
                if (nextIndex < 0) nextIndex = 0;
                switchTab(openFiles.get(nextIndex));
            } else {
                isInternalChange = true;
                etCode.setText("");
                tvCurrentFile.setText("No file open");
                currentFileName = "";
                isDirty = false;
                isInternalChange = false;
                if (fileAdapter != null) {
                    fileAdapter.setActiveFile("");
                }
            }
        }
    }

    private void switchTab(String fileName) {
        if (fileName.equals(currentFileName)) {
            updateTabVisuals(fileName);
            return;
        }

        // Only auto-save on switch if Auto-Save is actually enabled
        if (isDirty && !currentFileName.isEmpty() && isAutoSaveEnabled) {
            saveFile(currentFileName, etCode.getText().toString(), false);
        }

        currentFileName = fileName;
        tvCurrentFile.setText(fileName);
        loadFile(fileName, -1);
        
        if (fileAdapter != null) {
            fileAdapter.setActiveFile(fileName);
        }

        updateTabVisuals(fileName);
    }

    private void updateTabVisuals(String activeFileName) {
        for (int i = 0; i < llTabs.getChildCount(); i++) {
            View child = llTabs.getChildAt(i);
            child.setBackgroundColor(child.getTag().equals(activeFileName) ? 
                ContextCompat.getColor(this, R.color.pro_surface) : 
                ContextCompat.getColor(this, R.color.pro_header_footer));
        }
    }

    private void loadFile(String fileName) {
        loadFile(fileName, -1);
    }

    private void loadFile(String fileName, int jumpToLine) {
        fileExecutor.execute(() -> {
            StringBuilder sb = new StringBuilder();
            boolean success = false;
            try (FileInputStream fis = openFileInput(fileName);
                 BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
                success = true;
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Failed to load file: " + fileName, e);
            }

            final boolean finalSuccess = success;
            final String content = sb.toString();
            runOnUiThread(() -> {
                if (finalSuccess) {
                    isInternalChange = true;
                    etCode.setText(content);
                    isDirty = false;
                    autoSaveHandler.removeCallbacks(autoSaveRunnable);
                    
                    if (jumpToLine > 0) {
                        scrollToLine(jumpToLine);
                    }
                    
                    isInternalChange = false;
                }
            });
        });
    }

    private void scrollToLine(int line) {
        etCode.post(() -> {
            String text = etCode.getText().toString();
            String[] lines = text.split("\n");
            int pos = 0;
            for (int i = 0; i < Math.min(line - 1, lines.length); i++) {
                pos += lines[i].length() + 1;
            }
            etCode.setSelection(Math.min(pos, text.length()));
            etCode.requestFocus();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_dashboard) startActivity(new Intent(this, DashboardActivity.class));
        else if (id == R.id.nav_search) startActivity(new Intent(this, GlobalSearchActivity.class));
        else if (id == R.id.nav_new_project) {
            templateLauncher.launch(new Intent(this, TemplatesActivity.class));
        }
        else if (id == R.id.nav_cloud_sync) {
            Toast.makeText(this, "FTP/Cloud Sync is coming soon in the next update!", Toast.LENGTH_LONG).show();
        }
        else if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
        else if (id == R.id.nav_run) {
            Intent intent = new Intent(this, PreviewActivity.class);
            intent.putExtra("CODE", etCode.getText().toString());
            startActivity(intent);
        }
        else if (id == R.id.nav_editor) {
            // Already in Editor
        }
        else if (id == R.id.nav_database) startActivity(new Intent(this, DatabaseActivity.class));
        else if (id == R.id.nav_templates) {
            templateLauncher.launch(new Intent(this, TemplatesActivity.class));
        }
        else if (id == R.id.nav_open_project) showOpenFilePicker();
        else if (id == R.id.nav_save_as) saveFileAs(currentFileName);
        else if (id == R.id.nav_export) exportProjectAsZip();
        else if (id == R.id.nav_feedback) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + Uri.encode(getString(R.string.developer_email))));
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
            intent.putExtra(Intent.EXTRA_TEXT, "\n\n--- Sent from And-Ide Navigation ---\nVersion: 1.1.3\n");
            try {
                startActivity(intent);
            } catch (Exception e) {
                Intent fallback = new Intent(Intent.ACTION_SEND);
                fallback.setType("message/rfc822");
                fallback.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.developer_email)});
                fallback.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
                try {
                    startActivity(Intent.createChooser(fallback, "Send feedback via..."));
                } catch (Exception e2) {
                    Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
                }
            }
        }
        else if (id == R.id.nav_ai_chat) {
            if (isAiEnabled) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("CURRENT_FILE", currentFileName);
                intent.putExtra("FILE_CONTENT", etCode.getText().toString());
                startActivity(intent);
            } else {
                Toast.makeText(this, "AI Access is disabled in Settings", Toast.LENGTH_SHORT).show();
            }
        }
        else if (id == R.id.nav_settings) startActivity(new Intent(this, SettingsActivity.class));
        else if (id == R.id.nav_tutorial) startActivity(new Intent(this, TutorialActivity.class));
        else if (id == R.id.nav_live_preview) toggleLivePreview();
        else if (id == R.id.nav_about) startActivity(new Intent(this, AboutActivity.class));
        else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            getSharedPreferences("AndIde_Prefs", MODE_PRIVATE).edit().clear().apply();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void updateLineNumbers() {
        int lineCount = etCode.getLineCount();
        if (lineCount <= 0) lineCount = 1;

        StringBuilder lines = new StringBuilder();
        for (int i = 1; i <= lineCount; i++) {
            lines.append(i).append("\n");
        }
        tvLineNumbers.setText(lines.toString());
    }

    private void applyHighlighting(Editable s) {
        isHighlighting = true;
        // Remove existing spans
        Object[] spans = s.getSpans(0, s.length(), Object.class);
        for (Object span : spans) {
            if (span instanceof android.text.style.ForegroundColorSpan) {
                s.removeSpan(span);
            }
        }

        // Simple PHP Tag highlighting
        highlightPattern(s, "<\\?php|\\?>", ContextCompat.getColor(this, R.color.syntax_php_tag));

        // HTML Tags
        highlightPattern(s, "<[^>]+>", ContextCompat.getColor(this, R.color.syntax_html_tag));

        // Keywords (PHP/JS)
        highlightPattern(s, "\\b(function|return|if|else|for|while|foreach|class|public|private|protected|static|echo|print|var|let|const)\\b", ContextCompat.getColor(this, R.color.syntax_keyword));

        // Functions
        highlightPattern(s, "\\b[a-zA-Z_][a-zA-Z0-9_]*(?=\\s*\\()", ContextCompat.getColor(this, R.color.syntax_function));

        // Variables
        highlightPattern(s, "\\$[a-zA-Z_\\x7f-\\xff][a-zA-Z0-9_\\x7f-\\xff]*", ContextCompat.getColor(this, R.color.syntax_variable));

        // Strings
        highlightPattern(s, "\"[^\"]*\"|'[^']*'", ContextCompat.getColor(this, R.color.syntax_string));

        // Comments
        highlightPattern(s, "//.*|/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", ContextCompat.getColor(this, R.color.syntax_comment));

        isHighlighting = false;
    }

    private void highlightPattern(Editable s, String pattern, int color) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(s.toString());
        while (m.find()) {
            s.setSpan(new android.text.style.ForegroundColorSpan(color), m.start(), m.end(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
    private void performDebug() {
        tvDebugInfo.setVisibility(View.VISIBLE);
        tvDebugInfo.setText("Analyzing code for potential issues...\n");
        
        String code = etCode.getText().toString();
        boolean hasWarnings = false;

        if (code.contains("eval(") || code.contains("exec(")) {
            tvDebugInfo.append("[WARNING] Potential security risk: Use of eval() or exec() detected.\n");
            hasWarnings = true;
        }
        
        if (code.contains("mysql_")) {
            tvDebugInfo.append("[DEPRECATED] Use of old mysql_* functions. Please use PDO or mysqli.\n");
            hasWarnings = true;
        }

        if (hasWarnings && isAiEnabled) {
            tvDebugInfo.append("\nIssues found. Asking DDee for a proposed fix...");
            new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                intent.putExtra("CURRENT_FILE", currentFileName);
                intent.putExtra("AUTO_FIX_PROMPT", "I have some security/deprecation warnings in my code. Can you help me fix them?\n\nFile: " + currentFileName + "\n\nCode:\n```\n" + code + "\n```");
                startActivity(intent);
                tvDebugInfo.setVisibility(View.GONE);
            }, 2000);
        } else {
            tvDebugInfo.append("Analysis complete. No critical issues found that require AI assistance.\n");
            new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> tvDebugInfo.setVisibility(View.GONE), 5000);
        }
    }

    private void formatCode() {
        // Simple indentation formatter
        String code = etCode.getText().toString();
        String[] lines = code.split("\n");
        StringBuilder formatted = new StringBuilder();
        int indentLevel = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.endsWith("}") || line.startsWith("}")) indentLevel = Math.max(0, indentLevel - 1);
            
            for (int i = 0; i < indentLevel; i++) formatted.append("    ");
            formatted.append(line).append("\n");
            
            if (line.endsWith("{") || line.startsWith("{")) indentLevel++;
        }
        
        isInternalChange = true;
        etCode.setText(formatted.toString());
        isInternalChange = false;
        
        isDirty = true;
        Toast.makeText(this, "Code Formatted", Toast.LENGTH_SHORT).show();
    }

    private void showCodeHistory() {
        SQLiteDatabase db = DatabaseHelper.getInstance(this).getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_HISTORY,
                new String[]{DatabaseHelper.COLUMN_CODE_CONTENT, DatabaseHelper.COLUMN_TIMESTAMP},
                DatabaseHelper.COLUMN_FILE_NAME + " = ?",
                new String[]{currentFileName},
                null, null,
                DatabaseHelper.COLUMN_TIMESTAMP + " DESC",
                "20");

        List<String> historyContents = new ArrayList<>();
        List<String> historyLabels = new ArrayList<>();

        while (cursor.moveToNext()) {
            String content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CODE_CONTENT));
            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP));
            historyContents.add(content);
            historyLabels.add(timestamp);
        }
        cursor.close();

        if (historyContents.isEmpty()) {
            Toast.makeText(this, "No history found for " + currentFileName, Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Restore Version (" + currentFileName + ")")
                .setItems(historyLabels.toArray(new String[0]), (dialog, which) -> {
                    isInternalChange = true;
                    etCode.setText(historyContents.get(which));
                    isInternalChange = false;
                    isDirty = true;
                    Toast.makeText(this, "Restored version from " + historyLabels.get(which), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSnippets() {
        if (llSnippets.getVisibility() == View.VISIBLE) {
            llSnippets.setVisibility(View.GONE);
        } else {
            llSnippets.setVisibility(View.VISIBLE);
        }
    }

    private void shareCode() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, etCode.getText().toString());
        sendIntent.setType("text/plain");
        Intent shareIntent = Intent.createChooser(sendIntent, "Share Code via");
        startActivity(shareIntent);
    }

    // Database Bridge for the IDE
    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @android.webkit.JavascriptInterface
        public String executeSql(String sql) {
            try {
                String trimmedSql = sql.trim().toLowerCase();
                // Security: Block access to auth tables and system schema via word-boundary regex
                if (trimmedSql.matches(".*\\b(users|code_history|snippets|chat_history|sqlite_master|sqlite_temp_master|sqlite_sequence|drop|alter|truncate|delete)\\b.*")) {
                    android.util.Log.w("SecurityAudit", "Blocked unauthorized SQL access attempt: " + sql);
                    return "[{\"error\":\"Security Exception: Unauthorized database operation on internal tables.\"}]";
                }

                SQLiteDatabase db = DatabaseHelper.getInstance(mContext).getWritableDatabase();
                if (trimmedSql.startsWith("select")) {
                    Cursor cursor = db.rawQuery(sql, null);
                    org.json.JSONArray jsonArray = new org.json.JSONArray();
                    int columnCount = cursor.getColumnCount();
                    while (cursor.moveToNext()) {
                        org.json.JSONObject row = new org.json.JSONObject();
                        for (int i = 0; i < columnCount; i++) {
                            row.put(cursor.getColumnName(i), cursor.getString(i));
                        }
                        jsonArray.put(row);
                    }
                    cursor.close();
                    return jsonArray.toString();
                } else {
                    db.execSQL(sql);
                    return "[{\"status\":\"success\"}]";
                }
            } catch (Exception e) {
                return "[{\"error\":\"" + e.getMessage() + "\"}]";
            }
        }
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences("AndIde_Prefs", MODE_PRIVATE);
        
        // Load font size
        String fontSizeStr = prefs.getString("editor_font_size", "medium");
        float fontSize;
        switch (fontSizeStr) {
            case "small": fontSize = 12f; break;
            case "big": fontSize = 20f; break;
            default: fontSize = 16f; break; // medium
        }
        etCode.setTextSize(fontSize);
        tvLineNumbers.setTextSize(fontSize);

        // Load Show Line Numbers
        boolean showLineNumbers = prefs.getBoolean("show_line_numbers", true);
        tvLineNumbers.setVisibility(showLineNumbers ? View.VISIBLE : View.GONE);
        findViewById(R.id.line_divider).setVisibility(showLineNumbers ? View.VISIBLE : View.GONE);

        // Load Auto-Save and AI Access
        isAutoSaveEnabled = prefs.getBoolean("auto_save", true);
        isAiEnabled = prefs.getBoolean("ai_file_access", false);
    }

    private void setupCustomDrawer() {
        llCustomMenu = findViewById(R.id.llCustomMenu);
        RecyclerView rvFiles = findViewById(R.id.rvFiles);
        View btnNewFile = findViewById(R.id.btnNewFile);
        ivNavProfile = findViewById(R.id.ivNavProfile);
        tvNavName = findViewById(R.id.tvNavName);
        tvNavEmail = findViewById(R.id.tvNavEmail);

        updateNavHeader();

        if (btnNewFile != null) {
            btnNewFile.setOnClickListener(v -> showNewFileDialog());
        }

        if (rvFiles != null) {
            rvFiles.setLayoutManager(new LinearLayoutManager(this));
            fileList = new ArrayList<>();
            refreshFileList();
            fileAdapter = new FileAdapter(fileList, file -> {
                addTab(file.getName());
                drawerLayout.closeDrawer(GravityCompat.START);
            }, file -> {
                deleteFile(file);
            });
            fileAdapter.setActiveFile(currentFileName);
            rvFiles.setAdapter(fileAdapter);
        }
        
        populateCustomMenu();
    }

    private void populateCustomMenu() {
        llCustomMenu.removeAllViews();
        // Create a popup menu to easily inflate the existing menu resource
        android.widget.PopupMenu p  = new android.widget.PopupMenu(this, null);
        android.view.Menu menu = p.getMenu();
        getMenuInflater().inflate(R.menu.drawer_menu, menu);

        for (int i = 0; i < menu.size(); i++) {
            final android.view.MenuItem item = menu.getItem(i);
            if (item.isVisible()) {
                addMenuItem(item);
            }
        }
    }

    private void addMenuItem(final android.view.MenuItem item) {
        // Inflate a custom layout for menu items to have better control
        View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, llCustomMenu, false);
        TextView tv = view.findViewById(android.R.id.text1);
        tv.setText(item.getTitle());
        tv.setTextColor(ContextCompat.getColor(this, R.color.pro_on_surface));
        tv.setTextSize(14);
        
        // Add icon if available
        if (item.getIcon() != null) {
            tv.setCompoundDrawablesWithIntrinsicBounds(item.getIcon(), null, null, null);
            tv.setCompoundDrawablePadding((int)(16 * getResources().getDisplayMetrics().density));
            androidx.core.graphics.drawable.DrawableCompat.setTint(item.getIcon(), ContextCompat.getColor(this, R.color.pro_accent));
        }

        view.setOnClickListener(v -> {
            onNavigationItemSelected(item);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        
        llCustomMenu.addView(view);
    }

    private void setupFileExplorer(NavigationView nv) {
        // This is no longer used but kept to avoid breaking existing calls if any
    }

    private void showNewFileDialog() {
        EditText etFileName = new EditText(this);
        etFileName.setHint("index.php");
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("New File")
                .setView(etFileName)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etFileName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        saveFile(name, "", true);
                        refreshFileList();
                        switchTab(name);
                        addTab(name);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshFileList() {
        fileExecutor.execute(() -> {
            File dir = getFilesDir();
            File[] files = dir.listFiles();
            final List<File> newList = new ArrayList<>();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) newList.add(file);
                }
            }
            runOnUiThread(() -> {
                if (fileAdapter != null) {
                    fileAdapter.updateFiles(newList);
                }
            });
        });
    }

    private void deleteFile(File file) {
        SharedPreferences prefs = getSharedPreferences("AndIde_Prefs", MODE_PRIVATE);
        boolean confirmDelete = prefs.getBoolean("confirm_delete", true);

        if (confirmDelete) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete File")
                    .setMessage("Are you sure you want to delete " + file.getName() + "?")
                    .setPositiveButton("Delete", (dialog, which) -> performDeletion(file))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            performDeletion(file);
        }
    }

    private void performDeletion(File file) {
        String fileName = file.getName();
        fileExecutor.execute(() -> {
            boolean success = file.delete();
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(MainActivity.this, "File deleted from disk: " + fileName, Toast.LENGTH_SHORT).show();
                    refreshFileList();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to delete " + fileName, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void updateNavHeader() {
        if (userEmail == null) return;
        Cursor cursor = dbHelper.getUser(userEmail);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME));
            tvNavName.setText(name);
            tvNavEmail.setText(userEmail);

            byte[] img = cursor.getBlob(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROFILE_IMAGE));
            if (img != null && ivNavProfile != null) {
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(img, 0, img.length);
                ivNavProfile.setImageBitmap(bitmap);
            }
            cursor.close();
        }
    }

    private void setupSnippets() {
        // Quick access symbols for coding speed
        String[] symbols = {"{", "}", "(", ")", "[", "]", "$", "->", "=>", ";", "=", "+", "-", "*", "/", "<", ">", "!", "&", "|", "\"", "'"};
        float density = getResources().getDisplayMetrics().density;
        
        for (String sym : symbols) {
            com.google.android.material.button.MaterialButton btn = new com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle);
            btn.setText(sym);
            btn.setAllCaps(false);
            btn.setTextSize(14);
            btn.setPadding((int)(8 * density), 0, (int)(8 * density), 0);
            btn.setMinimumWidth(0);
            btn.setMinWidth(0);
            btn.setCornerRadius((int)(6 * density));
            btn.setTextColor(ContextCompat.getColor(this, R.color.pro_on_surface));
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.pro_surface)));
            btn.setStrokeWidth(1);
            btn.setStrokeColor(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.pro_on_surface_dim)));
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (int) (32 * density)); // Reduced height
            params.setMargins((int)(4 * density), (int)(2 * density), (int)(4 * density), (int)(2 * density));
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> {
                int start = etCode.getSelectionStart();
                etCode.getText().insert(start, sym);
            });
            llSnippets.addView(btn);
        }

        // Vertical divider between symbols and snippets
        View divider = new View(this);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(2, (int)(32 * density));
        divParams.setMargins((int)(8 * density), 0, (int)(8 * density), 0);
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.pro_accent));
        llSnippets.addView(divider);

        String[][] snippets = {
            {"<?php", "<?php\n\n?>"},
            {"echo", "echo \"\";"},
            {"if", "if (condition) {\n\n}"},
            {"for", "for ($i = 0; $i < count; $i++) {\n\n}"},
            {"html", "<!DOCTYPE html>\n<html>\n<head>\n<title></title>\n</head>\n<body>\n\n</body>\n</html>"},
            {"css", "<style>\n\n</style>"},
            {"js", "<script>\n\n</script>"}
        };

        for (String[] snippet : snippets) {
            com.google.android.material.button.MaterialButton btn = new com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle);
            btn.setText(snippet[0]);
            btn.setAllCaps(false);
            btn.setTextSize(11);
            btn.setPadding((int)(10 * density), 0, (int)(10 * density), 0);
            btn.setMinimumWidth(0);
            btn.setMinWidth(0);
            btn.setCornerRadius((int)(6 * density));
            btn.setTextColor(ContextCompat.getColor(this, R.color.pro_on_surface));
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.pro_header_footer)));
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (int) (32 * density)); // Reduced height
            params.setMargins((int)(4 * density), (int)(2 * density), (int)(4 * density), (int)(2 * density));
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> {
                int start = etCode.getSelectionStart();
                etCode.getText().insert(start, snippet[1]);
            });
            llSnippets.addView(btn);
        }
        // Always show the snippet/symbol bar for developer convenience
        llSnippets.setVisibility(View.VISIBLE);
    }
}
