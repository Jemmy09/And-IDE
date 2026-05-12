package com.AndIde.app;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.Intent;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private EditText etChatInput;
    private ImageButton btnSendChat;
    private LinearLayout llSkeleton;
    private DatabaseHelper dbHelper;
    private String userEmail;
    private String userName = "Developer";
    private String currentFile = "index.php";
    
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;
    private final String GEMINI_MODEL = "gemini-1.5-flash";
    private final String GROQ_API_KEY = BuildConfig.GROQ_API_KEY;
    private final String GROQ_MODEL = "llama-3.1-8b-instant";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = findViewById(R.id.chat_toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chat_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.chat_toolbar).setPadding(0, insets.top, 0, 0);
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        etChatInput = findViewById(R.id.etChatInput);
        btnSendChat = findViewById(R.id.btnSendChat);
        llSkeleton = findViewById(R.id.llSkeleton);

        dbHelper = DatabaseHelper.getInstance(this);
        userEmail = getSharedPreferences("AndIde_Prefs", MODE_PRIVATE).getString("logged_in_email", "guest");
        
        String intentFile = getIntent().getStringExtra("CURRENT_FILE");
        if (intentFile != null) {
            currentFile = intentFile;
        }

        loadUserName();
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        loadChatHistory();

        // Initial Greeting if history is empty
        if (chatMessages.isEmpty()) {
            addChatMessage("Hi! I'm DDe, your Senior Web Development Assistant. I'm here to help you build amazing websites using PHP, HTML, CSS, JS, and SQL. What are we building today?", false);
        }

        // Check for Auto-Fix Prompt from Editor
        String autoFixPrompt = getIntent().getStringExtra("AUTO_FIX_PROMPT");
        if (autoFixPrompt != null && !autoFixPrompt.isEmpty()) {
            sendMessage(autoFixPrompt);
        }

        // Check for "Explain Code" Request
        String explainCode = getIntent().getStringExtra("EXPLAIN_CODE");
        if (explainCode != null && !explainCode.isEmpty()) {
            sendMessage("Explain this code in plain English:\n\n```\n" + explainCode + "\n```");
        }

        btnSendChat.setOnClickListener(v -> {
            String prompt = etChatInput.getText().toString().trim();
            if (!prompt.isEmpty()) {
                sendMessage(prompt);
            }
        });
    }

    private void loadUserName() {
        Cursor cursor = dbHelper.getUser(userEmail);
        if (cursor != null && cursor.moveToFirst()) {
            userName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME));
            cursor.close();
        }
    }

    private void sendMessage(String prompt) {
        if (prompt.isEmpty()) return;
        
        addChatMessage(prompt, true);
        etChatInput.setText("");

        // Show Skeleton
        llSkeleton.setVisibility(View.VISIBLE);
        chatRecyclerView.setVisibility(View.GONE);

        // Logic to detect image generation request
        if (prompt.toLowerCase(Locale.ROOT).contains("generate image") || prompt.toLowerCase(Locale.ROOT).contains("draw")) {
            generateImage(prompt);
            return;
        }

        // Check if file access is enabled
        boolean hasFileAccess = getSharedPreferences("AndIde_Prefs", MODE_PRIVATE).getBoolean("ai_file_access", false);
        String accessText = hasFileAccess ? " You have permission to suggest file modifications." : " Read-only mode.";
        
        // Project Context (Recursive list of all files)
        String projectContext = getProjectStructure();

        // Contextual code snippet if available in intent
        String currentCode = getIntent().getStringExtra("FILE_CONTENT");
        String codeContext = (currentCode != null && !currentCode.isEmpty()) ? 
            "\n\nCURRENT CODE CONTENT of " + currentFile + ":\n```\n" + currentCode + "\n```" : "";

        // Construct JSON for Gemini API
        JsonObject jsonBody = new JsonObject();
        
        // System Instruction (Gemini 1.5+)
        JsonObject systemInstruction = new JsonObject();
        JsonArray systemParts = new JsonArray();
        JsonObject systemTextPart = new JsonObject();
        systemTextPart.addProperty("text", "You are DDe, a Senior Web Developer. " +
                "You are integrated into 'And-Ide'. " + projectContext + 
                "\nActive file: " + currentFile + ". " +
                "Focus: PHP, HTML, CSS, JS, SQL." + accessText + codeContext);
        systemParts.add(systemTextPart);
        systemInstruction.add("parts", systemParts);
        jsonBody.add("system_instruction", systemInstruction);

        JsonArray contents = new JsonArray();
        
        // Add history (limit to last 10 messages)
        int startIdx = Math.max(0, chatMessages.size() - 11);
        for (int i = startIdx; i < chatMessages.size(); i++) {
            ChatMessage msg = chatMessages.get(i);
            if (msg.isImage()) continue;
            
            JsonObject contentObj = new JsonObject();
            contentObj.addProperty("role", msg.isUser() ? "user" : "model");
            JsonArray parts = new JsonArray();
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", msg.getText());
            parts.add(textPart);
            contentObj.add("parts", parts);
            contents.add(contentObj);
        }
        
        jsonBody.add("contents", contents);

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL + ":generateContent?key=" + GEMINI_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // If Gemini fails, try Groq as fallback
                tryGroqFallback(jsonBody.toString(), hasFileAccess);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful() && !responseData.isEmpty()) {
                    JsonObject jsonResponse = gson.fromJson(responseData, JsonObject.class);
                    String aiResponse = jsonResponse.getAsJsonArray("candidates")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("content")
                            .getAsJsonArray("parts")
                            .get(0).getAsJsonObject()
                            .get("text").getAsString();
                    
                    runOnUiThread(() -> {
                        llSkeleton.setVisibility(View.GONE);
                        chatRecyclerView.setVisibility(View.VISIBLE);
                        addChatMessage(aiResponse, false);
                        
                        // If file access is enabled and AI provides a code block, show preview
                        if (hasFileAccess && aiResponse.contains("```")) {
                            showAiPreview(aiResponse);
                        }
                    });
                } else {
                    // Try Groq as fallback on Gemini API error
                    tryGroqFallback(jsonBody.toString(), hasFileAccess);
                }
            }
        });
    }

    private void tryGroqFallback(String originalPrompt, boolean hasFileAccess) {
        // Construct JSON for Groq API
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("model", GROQ_MODEL);
        
        JsonArray messages = new JsonArray();
        
        String accessText = hasFileAccess ? " You have permission to suggest file modifications." : " Read-only mode.";
        
        // Project Context (Recursive list of all files)
        String projectContext = getProjectStructure();

        String codeContext = getIntent().getStringExtra("FILE_CONTENT") != null ? "\n\n(Code Context included)" : "";

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "You are DDe, a Senior Web Developer. " +
                "You are integrated into 'And-Ide'. " + projectContext +
                "Focus: PHP, HTML, CSS, JS, SQL." + accessText + codeContext);
        messages.add(systemMessage);

        // Add history (limit to last 5 messages for fallback efficiency)
        int startIdx = Math.max(0, chatMessages.size() - 6);
        for (int i = startIdx; i < chatMessages.size(); i++) {
            ChatMessage msg = chatMessages.get(i);
            if (msg.isImage()) continue;
            JsonObject historyMsg = new JsonObject();
            historyMsg.addProperty("role", msg.isUser() ? "user" : "assistant");
            historyMsg.addProperty("content", msg.getText());
            messages.add(historyMsg);
        }
        
        jsonBody.add("messages", messages);

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + GROQ_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    llSkeleton.setVisibility(View.GONE);
                    chatRecyclerView.setVisibility(View.VISIBLE);
                    addChatMessage("Both AI services are currently unavailable. Check your connection.", false);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful() && !responseData.isEmpty()) {
                    JsonObject jsonResponse = gson.fromJson(responseData, JsonObject.class);
                    String aiResponse = jsonResponse.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();
                    
                    runOnUiThread(() -> {
                        llSkeleton.setVisibility(View.GONE);
                        chatRecyclerView.setVisibility(View.VISIBLE);
                        addChatMessage(aiResponse, false);
                        if (hasFileAccess && aiResponse.contains("```")) {
                            showAiPreview(aiResponse);
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        llSkeleton.setVisibility(View.GONE);
                        chatRecyclerView.setVisibility(View.VISIBLE);
                        addChatMessage("Crital Error: All AI engines returned " + response.code(), false);
                    });
                }
            }
        });
    }

    private void showAiPreview(String aiResponse) {
        // Simple regex to extract code inside ``` blocks
        Pattern pattern = Pattern.compile("```(?:\\w+)?\\n([\\s\\S]*?)```");
        Matcher matcher = pattern.matcher(aiResponse);
        if (matcher.find()) {
            String extractedCode = matcher.group(1);
            Intent intent = new Intent(this, AiPreviewActivity.class);
            intent.putExtra("PROPOSED_CODE", extractedCode);
            intent.putExtra("TARGET_FILE", currentFile);
            startActivity(intent);
        }
    }

    private void generateImage(String prompt) {
        // We use a free-tier stable diffusion API or DALL-E if key is provided
        // Use Polling via Pollinations.ai (Free/Fast)
        String imageUrl = "https://image.pollinations.ai/prompt/" + prompt.replace(" ", "%20") + "?width=1024&height=1024&nologo=true";
        
        new android.os.Handler().postDelayed(() -> {
            runOnUiThread(() -> {
                llSkeleton.setVisibility(View.GONE);
                chatRecyclerView.setVisibility(View.VISIBLE);
                addChatImage(imageUrl);
            });
        }, 3000);
    }

    private void addChatImage(String url) {
        chatMessages.add(new ChatMessage(url, false, true));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        saveChatMessage("IMAGE_URL:" + url, false);
    }

    private void addChatMessage(String text, boolean isUser) {
        chatMessages.add(new ChatMessage(text, isUser));
        chatAdapter.updateMessages(chatMessages);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        saveChatMessage(text, isUser);
    }

    private void saveChatMessage(String text, boolean isUser) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_CHAT_USER_EMAIL, userEmail);
        values.put(DatabaseHelper.COLUMN_CHAT_MESSAGE, text);
        values.put(DatabaseHelper.COLUMN_CHAT_IS_USER, isUser ? 1 : 0);
        db.insert(DatabaseHelper.TABLE_CHAT_HISTORY, null, values);
    }

    private void loadChatHistory() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_CHAT_HISTORY,
                null,
                DatabaseHelper.COLUMN_CHAT_USER_EMAIL + " = ?",
                new String[]{userEmail},
                null, null,
                DatabaseHelper.COLUMN_CHAT_TIMESTAMP + " ASC");

        while (cursor.moveToNext()) {
            String message = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CHAT_MESSAGE));
            boolean isUser = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CHAT_IS_USER)) == 1;
            
            if (message.startsWith("IMAGE_URL:")) {
                chatMessages.add(new ChatMessage(message.replace("IMAGE_URL:", ""), false, true));
            } else {
                chatMessages.add(new ChatMessage(message, isUser));
            }
        }
        cursor.close();
        chatAdapter.updateMessages(chatMessages);
        if (!chatMessages.isEmpty()) {
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        }
    }

    private String getProjectStructure() {
        StringBuilder sb = new StringBuilder("\nPROJECT STRUCTURE:\n");
        buildProjectStructure(getFilesDir(), "", sb);
        return sb.toString();
    }

    private void buildProjectStructure(File dir, String prefix, StringBuilder sb) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    sb.append(prefix).append("[").append(f.getName()).append("/]\n");
                    buildProjectStructure(f, prefix + "  ", sb);
                } else {
                    sb.append(prefix).append("- ").append(f.getName()).append("\n");
                }
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
