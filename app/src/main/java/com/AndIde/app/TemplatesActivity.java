package com.AndIde.app;

import android.content.Intent;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TemplatesActivity extends AppCompatActivity {

    private RecyclerView rvTemplates;
    private TemplateAdapter adapter;
    private List<Template> templateList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_templates);

        Toolbar toolbar = findViewById(R.id.templates_toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.templates_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.templates_toolbar).setPadding(0, insets.top, 0, 0);
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvTemplates = findViewById(R.id.rvTemplates);
        rvTemplates.setLayoutManager(new LinearLayoutManager(this));

        loadTemplates();

        adapter = new TemplateAdapter(templateList, template -> {
            Intent intent = new Intent();
            intent.putExtra("CODE", template.getCode());
            intent.putExtra("FILENAME", template.getFileName());
            setResult(RESULT_OK, intent);
            finish();
        });

        rvTemplates.setAdapter(adapter);
    }

    private void loadTemplates() {
        templateList = new ArrayList<>();
        
        templateList.add(new Template(
            "DDee AI: Clean & Clear SQL",
            "A professional snippet for secure database interactions, provided by DDee AI.",
            "<?php\n/**\n * PROFESSIONAL SQL INTEGRATION BY DDEE AI\n * Clean, secure, and production-ready code.\n */\n\ntry {\n    // Database Connection Logic\n    $db = new PDO('sqlite:my_database.db');\n    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);\n\n    // Prepared Statement for Security (Prevents SQL Injection)\n    $stmt = $db->prepare(\"SELECT * FROM users WHERE status = :status\");\n    $stmt->execute(['status' => 'active']);\n    $results = $stmt->fetchAll(PDO::FETCH_ASSOC);\n\n    echo \"<h2 style='color: #22c55e;'>Professional Data Fetch Successful</h2>\";\n    echo \"<ul style='background: #1e293b; padding: 15px; border-radius: 8px; color: white; list-style: none;'>\";\n    foreach ($results as $row) {\n        echo \"<li style='border-bottom: 1px solid #334155; padding: 5px 0;'>User: \" . htmlspecialchars($row['username']) . \"</li>\";\n    }\n    echo \"</ul>\";\n\n} catch (PDOException $e) {\n    echo \"<p style='color: #ef4444;'>Error: \" . $e->getMessage() . \"</p>\";\n}\n?>",
            "pro_sql.php"
        ));

        templateList.add(new Template(
            "DDee AI: Pro Web Helper",
            "A showcase of DDee AI's expertise in full-stack web development.",
            "<?php\n/**\n * DDee AI - Professional Web Developer Assistant\n */\n$skills = ['PHP', 'MySQL', 'HTML', 'CSS', 'JavaScript'];\n?>\n<div style='font-family: sans-serif; background: #0f172a; color: #f8fafc; padding: 2rem; border-radius: 1rem; border: 1px solid #334155;'>\n    <h1 style='color: #38bdf8; margin-top: 0;'>DDee AI Assistant</h1>\n    <p style='font-size: 1.1rem; line-height: 1.6;'>I am your professional coding companion. I specialize in building robust backends and beautiful frontends.</p>\n    \n    <div style='margin: 1.5rem 0;'>\n        <?php foreach($skills as $skill): ?>\n            <span style='background: #1e293b; color: #38bdf8; padding: 0.4rem 0.8rem; border-radius: 0.5rem; margin-right: 0.5rem; font-size: 0.9rem; border: 1px solid #334155;'><?php echo $skill; ?></span>\n        <?php endforeach; ?>\n    </div>\n\n    <div style='background: #020617; padding: 1rem; border-radius: 0.5rem; border-left: 4px solid #38bdf8;'>\n        <code style='color: #94a3b8;'>// Let's build something amazing together like a pro.</code>\n    </div>\n</div>",
            "ddee_ai.php"
        ));

        templateList.add(new Template(
            "Full Stack: User Dashboard",
            "Fetches data from a database and displays it using a professional card-based PHP layout.",
            "<?php\n/**\n * PROFESSIONAL DASHBOARD BY DDEE AI\n * Clean, mobile-responsive data visualization.\n */\n\n// 1. Database Connection (PDO)\ntry {\n    $pdo = new PDO('sqlite:users.db');\n    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);\n\n    $stmt = $pdo->query(\"SELECT * FROM users ORDER BY id DESC LIMIT 5\");\n    $users = $stmt->fetchAll(PDO::FETCH_ASSOC);\n\n} catch (PDOException $e) {\n    $users = []; // Fallback for demo\n}\n\n// 2. Modern UI Styles\necho \"<style>\n    :root { --bg: #0f172a; --card: #1e293b; --accent: #38bdf8; --text: #f8fafc; }\n    body { background: var(--bg); color: var(--text); font-family: 'Inter', sans-serif; padding: 20px; }\n    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }\n    .grid { display: grid; gap: 15px; }\n    .card { background: var(--card); border: 1px solid #334155; padding: 15px; border-radius: 12px; transition: transform 0.2s; }\n    .card:active { transform: scale(0.98); }\n    .name { font-weight: 600; color: var(--accent); display: block; }\n    .email { font-size: 0.85em; opacity: 0.7; }\n    .badge { font-size: 10px; background: #0ea5e9; padding: 2px 8px; border-radius: 10px; float: right; }\n</style>\";\n\n// 3. Header\necho \"<div class='header'>\";\necho \"<h2 style='margin:0;'>Active Users</h2>\";\necho \"<span style='color:var(--accent); font-size:12px;'>DDee AI Verified</span>\";\necho \"</div>\";\n\n// 4. Data Display\necho \"<div class='grid'>\";\nif (empty($users)) {\n    echo \"<div class='card'>No users found. Let's start building!</div>\";\n} else {\n    foreach ($users as $user) {\n        echo \"<div class='card'>\";\n        echo \"<span class='badge'>User #\" . $user['id'] . \"</span>\";\n        echo \"<span class='name'>\" . htmlspecialchars($user['username']) . \"</span>\";\n        echo \"<span class='email'>\" . htmlspecialchars($user['email']) . \"</span>\";\n        echo \"</div>\";\n    }\n}\necho \"</div>\";\n?>",
            "dashboard.php"
        ));

        templateList.add(new Template(
            "Basic PHP Header",
            "A standard PHP starting point with error reporting enabled.",
            "<?php\n// Enable error reporting for development\nini_set('display_errors', 1);\nerror_reporting(E_ALL);\n\necho \"<h1>Hello World from And-Ide!</h1>\";\n?>",
            "index.php"
        ));

        templateList.add(new Template(
            "MySQL Connection (PDO)",
            "Securely connect to your database using PDO.",
            "<?php\n$host = 'localhost';\n$db   = 'AndIde';\n$user = 'root';\n$pass = '';\n$charset = 'utf8mb4';\n\n$dsn = \"mysql:host=$host;dbname=$db;charset=$charset\";\n$options = [\n    PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,\n    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,\n];\n\ntry {\n     $pdo = new PDO($dsn, $user, $pass, $options);\n     echo \"Connected successfully\";\n} catch (\\PDOException $e) {\n     throw new \\PDOException($e->getMessage(), (int)$e->getCode());\n}\n?>",
            "db_connect.php"
        ));

        templateList.add(new Template(
            "PHP Login Logic",
            "Simple session-based login validation.",
            "<?php\nsession_start();\n\nif ($_SERVER['REQUEST_METHOD'] == 'POST') {\n    $username = $_POST['username'];\n    $password = $_POST['password'];\n\n    // Example validation\n    if ($username === 'admin' && $password === '1234') {\n        $_SESSION['loggedin'] = true;\n        header('Location: dashboard.php');\n    } else {\n        $error = 'Invalid credentials';\n    }\n}\n?>",
            "login_process.php"
        ));

        templateList.add(new Template(
            "HTML/CSS Modern Layout",
            "A responsive HTML5 starter with internal CSS.",
            "<!DOCTYPE html>\n<html>\n<head>\n<style>\n  body { font-family: sans-serif; background: #f4f4f4; text-align: center; }\n  .card { background: white; padding: 20px; border-radius: 10px; margin: 50px auto; width: 300px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); }\n</style>\n</head>\n<body>\n  <div class='card'>\n    <h2>My Mobile Project</h2>\n    <p>Created with And-Ide</p>\n  </div>\n</body>\n</html>",
            "index.html"
        ));

        templateList.add(new Template(
            "Dev Portfolio",
            "A clean, professional portfolio template to showcase developer projects.",
            "<!DOCTYPE html>\n<html>\n<head>\n<style>\n  :root { --primary: #007ACC; --dark: #121212; --light: #E0E0E0; }\n  body { background: var(--dark); color: var(--light); font-family: 'Segoe UI', sans-serif; margin: 0; padding: 20px; }\n  .header { border-bottom: 2px solid var(--primary); padding-bottom: 10px; margin-bottom: 30px; }\n  .project-grid { display: grid; grid-template-columns: 1fr; gap: 20px; }\n  .project-card { background: #1E1E1E; border: 1px solid #333; padding: 20px; border-radius: 12px; transition: 0.3s; }\n  .project-card:hover { border-color: var(--primary); transform: translateY(-5px); }\n  .badge { background: var(--primary); padding: 4px 10px; border-radius: 20px; font-size: 0.7em; text-transform: uppercase; }\n  h1, h2 { color: var(--primary); }\n</style>\n</head>\n<body>\n  <div class='header'>\n    <h1>Developer Portfolio</h1>\n    <p>Web Developer & IT Professional</p>\n  </div>\n\n  <div class='project-grid'>\n    <div class='project-card'>\n      <span class='badge'>PHP / SQL</span>\n      <h2>Inventory System</h2>\n      <p>A mobile-optimized inventory manager with SQLite integration.</p>\n    </div>\n    \n    <div class='project-card'>\n      <span class='badge'>HTML / CSS</span>\n      <h2>Personal Blog</h2>\n      <p>Clean, minimal blog layout built using modern CSS variables.</p>\n    </div>\n  </div>\n\n  <footer style='margin-top:50px; text-align:center; opacity:0.5;'>\n    <p>Created with And-Ide Mobile Studio</p>\n  </footer>\n</body>\n</html>",
            "portfolio.html"
        ));

        templateList.add(new Template(
            "Database: User Registration",
            "A complete PHP/SQL example for registering new users into the database.",
            "<?php\n/**\n * WEB DEV: USER REGISTRATION\n * This script handles form submission and database insertion.\n */\n\n// 1. Database Logic (Simulation)\nif ($_SERVER['REQUEST_METHOD'] == 'POST') {\n    $name = htmlspecialchars($_POST['name']);\n    $email = htmlspecialchars($_POST['email']);\n    \n    // In And-Ide, this would run against your local SQLite database\n    // INSERT INTO users (name, email) VALUES ('$name', '$email');\n    \n    echo \"<div style='background:#4CAF50; padding:10px; border-radius:5px;'>\";\n    echo \"<strong>Success!</strong> Account created for $name ($email).\";\n    echo \"</div>\";\n}\n?>\n\n<form method='POST' style='background:#1e1e1e; padding:20px; border-radius:10px; color:white;'>\n  <h3>Create Account</h3>\n  Name: <input type='text' name='name' style='width:100%; margin:10px 0; background:#333; color:white; border:none; padding:8px;' required><br>\n  Email: <input type='email' name='email' style='width:100%; margin:10px 0; background:#333; color:white; border:none; padding:8px;' required><br>\n  <button type='submit' style='background:#007ACC; color:white; border:none; padding:10px 20px; cursor:pointer; width:100%;'>Register</button>\n</form>",
            "register.php"
        ));
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
