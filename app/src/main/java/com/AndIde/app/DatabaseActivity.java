package com.AndIde.app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseActivity extends AppCompatActivity {

    private EditText etSqlQuery;
    private Button btnExecuteSql;
    private TableLayout tlDbResults;
    private DatabaseHelper dbHelper;
    private boolean isHighlighting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_database);

        Toolbar toolbar = findViewById(R.id.db_toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.db_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.db_toolbar).setPadding(0, insets.top, 0, 0);
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = DatabaseHelper.getInstance(this);
        etSqlQuery = findViewById(R.id.etSqlQuery);
        btnExecuteSql = findViewById(R.id.btnExecuteSql);
        tlDbResults = findViewById(R.id.tlDbResults);

        setupWebDevAssistant();
        setupSqlHighlighting();

        btnExecuteSql.setOnClickListener(v -> executeSql());
        
        // Handle SQL from intent if coming from Smart Run
        String incomingSql = getIntent().getStringExtra("SQL_QUERY");
        if (incomingSql != null && !incomingSql.isEmpty()) {
            etSqlQuery.setText(incomingSql);
            executeSql();
        } else {
            // Load default table view
            etSqlQuery.setText("SELECT * FROM users;");
            executeSql();
        }
    }

    private void setupSqlHighlighting() {
        etSqlQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isHighlighting) {
                    applySqlHighlighting(s);
                }
            }
        });
        // Initial highlight
        applySqlHighlighting(etSqlQuery.getText());
    }

    private void applySqlHighlighting(Editable s) {
        isHighlighting = true;
        // Remove existing spans
        Object[] spans = s.getSpans(0, s.length(), android.text.style.ForegroundColorSpan.class);
        for (Object span : spans) {
            s.removeSpan(span);
        }

        String text = s.toString();

        // SQL Keywords
        highlightPattern(s, "(?i)\\b(SELECT|INSERT|UPDATE|DELETE|FROM|WHERE|CREATE|TABLE|DROP|ALTER|JOIN|ON|GROUP|BY|ORDER|LIMIT|OFFSET|VALUES|INTO|SET|PRIMARY|KEY|INTEGER|TEXT|REAL|BLOB|NULL|AND|OR|NOT|LIKE|IN|IS|AS|DISTINCT|COUNT|SUM|AVG|MIN|MAX|PRAGMA|EXPLAIN)\\b", 
                androidx.core.content.ContextCompat.getColor(this, R.color.syntax_keyword));

        // Strings
        highlightPattern(s, "'[^']*'", 
                androidx.core.content.ContextCompat.getColor(this, R.color.syntax_string));

        // Comments
        highlightPattern(s, "--.*|/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", 
                androidx.core.content.ContextCompat.getColor(this, R.color.syntax_comment));

        // Numbers
        highlightPattern(s, "\\b\\d+\\b", 
                androidx.core.content.ContextCompat.getColor(this, R.color.syntax_function)); // Reuse green for numbers

        isHighlighting = false;
    }

    private void highlightPattern(Editable s, String pattern, int color) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(s.toString());
        while (m.find()) {
            s.setSpan(new android.text.style.ForegroundColorSpan(color), m.start(), m.end(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void setupWebDevAssistant() {
        Button btnShowAll = findViewById(R.id.btnQuickShowAll);
        Button btnAddUser = findViewById(R.id.btnQuickAddUser);
        Button btnClear = findViewById(R.id.btnQuickClear);
        Button btnGlossary = findViewById(R.id.btnSqlGlossary);
        Button btnShowTables = findViewById(R.id.btnShowTables);
        Button btnCreateSchema = findViewById(R.id.btnCreateSchema);

        btnShowTables.setOnClickListener(v -> {
            etSqlQuery.setText("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android%';");
            executeSql();
        });

        btnCreateSchema.setOnClickListener(v -> {
            etSqlQuery.setText(getString(R.string.sql_products_schema));
            executeSql();
            Toast.makeText(this, "Sample web schema (products) ready!", Toast.LENGTH_SHORT).show();
        });

        btnShowAll.setOnClickListener(v -> {
            etSqlQuery.setText("SELECT * FROM users;");
            executeSql();
        });

        btnAddUser.setOnClickListener(v -> {
            etSqlQuery.setText("INSERT INTO users (name, email, password, birthday, age) VALUES ('DevUser', 'dev@andide.com', '1234', '1990-01-01', 34);");
            executeSql();
        });

        btnClear.setOnClickListener(v -> {
            etSqlQuery.setText("DELETE FROM users;");
            executeSql();
            Toast.makeText(this, "Table data cleared!", Toast.LENGTH_SHORT).show();
        });

        btnGlossary.setOnClickListener(v -> showSqlCheatSheet());
    }

    private void showSqlCheatSheet() {
        String cheatSheet = "SQL COMMANDS CHEAT SHEET\n\n" +
                "1. SELECT - Get data from a table\n" +
                "   Example: SELECT * FROM users;\n\n" +
                "2. INSERT - Add new data\n" +
                "   Example: INSERT INTO users (name) VALUES ('John');\n\n" +
                "3. UPDATE - Change existing data\n" +
                "   Example: UPDATE users SET name='Jane' WHERE id=1;\n\n" +
                "4. DELETE - Remove data\n" +
                "   Example: DELETE FROM users WHERE id=1;\n\n" +
                "5. CREATE TABLE - Make a new table\n" +
                "   Example: CREATE TABLE my_table (id INTEGER PRIMARY KEY, name TEXT);\n\n" +
                "6. WHERE - Filter results\n" +
                "   Example: SELECT * FROM users WHERE age > 18;";

        new android.app.AlertDialog.Builder(this)
                .setTitle("SQL Reference Guide")
                .setMessage(cheatSheet)
                .setPositiveButton("Got it!", null)
                .show();
    }

    private void executeSql() {
        String sql = etSqlQuery.getText().toString().trim();
        if (sql.isEmpty()) return;

        btnExecuteSql.setEnabled(false);
        tlDbResults.removeAllViews();

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            try {
                String trimmedSql = sql.trim().toLowerCase(Locale.ROOT);
                if (trimmedSql.startsWith("select") || trimmedSql.startsWith("pragma") || trimmedSql.startsWith("explain")) {
                    Cursor cursor = db.rawQuery(sql, null);
                    runOnUiThread(() -> {
                        displayResults(cursor);
                        btnExecuteSql.setEnabled(true);
                    });
                } else {
                    db.execSQL(sql);
                    runOnUiThread(() -> {
                        Toast.makeText(DatabaseActivity.this, "Query executed successfully", Toast.LENGTH_SHORT).show();
                        btnExecuteSql.setEnabled(true);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(DatabaseActivity.this, "SQL Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnExecuteSql.setEnabled(true);
                });
            }
        });
        executor.shutdown();
    }

    private void displayResults(Cursor cursor) {
        if (cursor == null || cursor.getCount() == 0) {
            TextView tv = new TextView(this);
            tv.setText("No results found or empty table.");
            tv.setPadding(20, 20, 20, 20);
            tv.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.pro_on_surface));
            tlDbResults.addView(tv);
            return;
        }

        // Header Row
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.pro_header_footer));
        String[] columnNames = cursor.getColumnNames();

        for (String col : columnNames) {
            TextView tv = createCell(col, true);
            headerRow.addView(tv);
        }
        tlDbResults.addView(headerRow);

        // Data Rows
        int count = 0;
        while (cursor.moveToNext() && count < 500) { // Limit to 500 rows for performance
            TableRow dataRow = new TableRow(this);
            dataRow.setBackgroundColor(count % 2 == 0 ? 
                androidx.core.content.ContextCompat.getColor(this, R.color.pro_surface) : 
                androidx.core.content.ContextCompat.getColor(this, R.color.pro_header_footer));
            
            for (int i = 0; i < columnNames.length; i++) {
                String val = cursor.getString(i);
                TextView tv = createCell(val != null ? val : "NULL", false);
                dataRow.addView(tv);
            }
            tlDbResults.addView(dataRow);
            count++;
        }
    }

    private TextView createCell(String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(24, 16, 24, 16);
        tv.setTextColor(isHeader ? 
                androidx.core.content.ContextCompat.getColor(this, R.color.pro_accent) : 
                androidx.core.content.ContextCompat.getColor(this, R.color.pro_on_surface));
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        tv.setTextSize(13);
        if (isHeader) {
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        }
        return tv;
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}