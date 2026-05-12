package com.AndIde.app;

import android.content.Intent;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GlobalSearchActivity extends AppCompatActivity {

    private EditText etGlobalSearch;
    private android.widget.ImageButton btnDoGlobalSearch;
    private TextView tvSearchResultCount;
    private RecyclerView rvSearchResults;
    private SearchResultAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_global_search);

        Toolbar toolbar = findViewById(R.id.search_toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.search_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.search_toolbar).setPadding(0, insets.top, 0, 0);
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Global Search");
        }

        etGlobalSearch = findViewById(R.id.etGlobalSearch);
        etGlobalSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
        btnDoGlobalSearch = findViewById(R.id.btnDoGlobalSearch);
        btnDoGlobalSearch.setOnClickListener(v -> performSearch());
        tvSearchResultCount = findViewById(R.id.tvSearchResultCount);
        rvSearchResults = findViewById(R.id.rvSearchResults);

        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void performSearch() {
        String query = etGlobalSearch.getText().toString().trim();
        if (query.isEmpty()) return;

        btnDoGlobalSearch.setEnabled(false);
        tvSearchResultCount.setText("Searching...");

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<SearchResult> results = new ArrayList<>();
            File dir = getFilesDir();
            File[] files = dir.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        searchInFile(file, query, results);
                    }
                }
            }

            final List<SearchResult> finalResults = results;
            runOnUiThread(() -> {
                btnDoGlobalSearch.setEnabled(true);
                tvSearchResultCount.setText("Results: " + finalResults.size());
                adapter = new SearchResultAdapter(finalResults, result -> {
                    Intent intent = new Intent(GlobalSearchActivity.this, MainActivity.class);
                    intent.putExtra("OPEN_FILE", result.fileName);
                    intent.putExtra("LINE_NUMBER", result.lineNumber);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                });
                rvSearchResults.setAdapter(adapter);
            });
        });
        executor.shutdown();
    }

    private void searchInFile(File file, String query, List<SearchResult> results) {
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                if (line.toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                    results.add(new SearchResult(file.getName(), line.trim(), lineNumber));
                }
                lineNumber++;
            }
        } catch (Exception e) {
            android.util.Log.e("GlobalSearch", "Error searching in file: " + file.getName(), e);
        }
    }

    public static class SearchResult {
        String fileName;
        String lineContent;
        int lineNumber;

        SearchResult(String fileName, String lineContent, int lineNumber) {
            this.fileName = fileName;
            this.lineContent = lineContent;
            this.lineNumber = lineNumber;
        }
    }

    private class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
        private List<SearchResult> results;
        private OnResultClickListener listener;

        SearchResultAdapter(List<SearchResult> results, OnResultClickListener listener) {
            this.results = results;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            SearchResult result = results.get(position);
            holder.text1.setText(result.fileName + " (Line " + result.lineNumber + ")");
            holder.text2.setText(result.lineContent);
            holder.text1.setTextColor(androidx.core.content.ContextCompat.getColor(GlobalSearchActivity.this, R.color.pro_accent));
            holder.text2.setTextColor(androidx.core.content.ContextCompat.getColor(GlobalSearchActivity.this, R.color.pro_on_surface_dim));
            holder.itemView.setOnClickListener(v -> listener.onResultClick(result));
        }

        @Override
        public int getItemCount() {
            return results.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(android.view.View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }

    interface OnResultClickListener {
        void onResultClick(SearchResult result);
    }
}