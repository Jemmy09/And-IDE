package com.AndIde.app;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class PreviewActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_preview);

        dbHelper = DatabaseHelper.getInstance(this);

        Toolbar toolbar = findViewById(R.id.preview_toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.preview_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.preview_toolbar).setPadding(0, insets.top, 0, 0);
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Preview");
        }

        WebView webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidDB");

        String code = getIntent().getStringExtra("CODE");
        if (code != null) {
            // Check if code contains PHP tags
            if (code.contains("<?php")) {
                runPhpCode(webView, code);
            } else {
                webView.loadDataWithBaseURL("https://and-ide.local/", code, "text/html", "UTF-8", null);
            }
        }
    }

    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public String executeSql(String sql) {
            try {
                String trimmedSql = sql.trim().toLowerCase();
                // Security: Block access to auth tables and system schema via word-boundary regex
                if (trimmedSql.matches(".*\\b(users|code_history|snippets|chat_history|sqlite_master|sqlite_temp_master|sqlite_sequence|drop|alter|truncate|delete)\\b.*")) {
                    android.util.Log.w("SecurityAudit", "Blocked unauthorized SQL access attempt in Preview: " + sql);
                    return "[{\"error\":\"Security Exception: Unauthorized database operation on internal tables.\"}]";
                }

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                if (trimmedSql.startsWith("select")) {
                    Cursor cursor = db.rawQuery(sql, null);
                    JSONArray jsonArray = new JSONArray();
                    int columnCount = cursor.getColumnCount();
                    while (cursor.moveToNext()) {
                        JSONObject row = new JSONObject();
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

    private void runPhpCode(WebView webView, String code) {
        int bgColor = androidx.core.content.ContextCompat.getColor(this, R.color.pro_surface);
        int textColor = androidx.core.content.ContextCompat.getColor(this, R.color.pro_on_surface);
        int accentColor = androidx.core.content.ContextCompat.getColor(this, R.color.pro_accent);
        int errorBg = androidx.core.content.ContextCompat.getColor(this, R.color.debug_error_bg);
        int errorText = androidx.core.content.ContextCompat.getColor(this, R.color.debug_error_text);

        String hexBg = String.format("#%06X", (0xFFFFFF & bgColor));
        String hexText = String.format("#%06X", (0xFFFFFF & textColor));
        String hexAccent = String.format("#%06X", (0xFFFFFF & accentColor));
        String hexErrBg = String.format("#%06X", (0xFFFFFF & errorBg));
        String hexErrText = String.format("#%06X", (0xFFFFFF & errorText));

        String phpRunnerHtml = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "   <meta charset=\"UTF-8\">\n" +
                "   <style>\n" +
                "       body { font-family: 'Consolas', 'Monaco', monospace; padding: 15px; background: " + hexBg + "; color: " + hexText + "; line-height: 1.5; }\n" +
                "       .header { color: " + hexAccent + "; border-bottom: 1px solid rgba(128,128,128,0.3); padding-bottom: 8px; margin-bottom: 15px; font-size: 12px; }\n" +
                "       pre { white-space: pre-wrap; word-wrap: break-word; }\n" +
                "       .error { color: " + hexErrText + "; background: " + hexErrBg + "; padding: 10px; border-radius: 4px; }\n" +
                "       .db-log { color: " + hexAccent + "; font-size: 11px; margin-top: 10px; border-top: 1px dashed rgba(128,128,128,0.3); padding-top: 5px; opacity: 0.8; }\n" +
                "   </style>\n" +
                "   <script src=\"https://cdn.jsdelivr.net/npm/@php-wasm/web@0.0.9/dist/index.iife.js\"></script>\n" +
                "</head>\n" +
                "<body>\n" +
                "   <div class=\"header\">PHP 8.2 + MySQL (Bridge) Mode</div>\n" +
                "   <div id=\"output\">Initializing Engine...</div>\n" +
                "   <div id=\"db-status\" class=\"db-log\">Database: Ready</div>\n" +
                "   <script>\n" +
                "       async function run() {\n" +
                "           const out = document.getElementById('output');\n" +
                "           try {\n" +
                "               const php = await PHP.loadPHP();\n" +
                "               \n" +
                "               // Define the SQL bridge for PHP\n" +
                "               php.registerFunction('exec_sql', (sql) => {\n" +
                "                   return window.AndroidDB ? AndroidDB.executeSql(sql) : '[]';\n" +
                "               });\n" +
                "\n" +
                "               let buffer = '';\n" +
                "               php.onMessage((msg) => { buffer += msg; });\n" +
                "               \n" +
                "               const userCode = `" + code.replace("`", "\\`").replace("\\", "\\\\").replace("$", "\\$") + "`;\n" +
                "               \n" +
                "               // Inject helper function\n" +
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

        webView.loadDataWithBaseURL("https://and-ide.local/", phpRunnerHtml, "text/html", "UTF-8", null);
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}