<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="128" height="128" alt="And-IDE Logo">
</p>

<h1 align="center">And-IDE</h1>

<p align="center">
  <i>A professional, mobile-first workspace for web development on the go.</i>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/STATUS-ACTIVE-success?style=for-the-badge" alt="Status">
  <img src="https://img.shields.io/badge/PLATFORM-ANDROID-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform">
  <img src="https://img.shields.io/badge/LANGUAGE-JAVA-007396?style=for-the-badge&logo=java&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/AI-GEMINI%20|%20GROQ-9333EA?style=for-the-badge" alt="AI">
  <img src="https://img.shields.io/badge/DATABASE-SQLITE-003B57?style=for-the-badge&logo=sqlite&logoColor=white" alt="Database">
</p>

---

## 🚀 Welcome to And-IDE

**And-IDE** is a powerful, lightweight Integrated Development Environment (IDE) built specifically for Android. It allows you to build, test, and preview web projects (HTML, CSS, JS, PHP) and manage SQL databases entirely from your mobile device. 

---

## 📲 How to Install on Android

To install this app without cloning the code or using a computer:

1.  **Visit Releases:** Go to the [Releases](https://github.com/Jemmy09/And-IDE/releases) tab on this GitHub page.
2.  **Download APK:** Download the latest `And-IDE.apk` file.
3.  **Install:** Open the file on your Android phone. If prompted, "Allow installation from unknown sources."
4.  **Start Coding:** Launch the app and enjoy!

---

## 📖 Full Usage Guide (Complete)

### 1. 🌐 HTML & CSS (The Structure)
Create an `index.html` file and a `style.css` file. 
**Example Code:**
```html
<!-- index.html -->
<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <h1>Hello from And-IDE!</h1>
    <p>This was built on my phone.</p>
</body>
</html>
```
```css
/* style.css */
body { background: #f0f0f0; text-align: center; font-family: sans-serif; }
h1 { color: #3DDC84; }
```

### 2. ✨ JavaScript (The Logic)
Add interactivity to your pages by creating a `script.js` file.
**Example Code:**
```javascript
function greet() {
    alert("Welcome to And-IDE!");
}
document.querySelector('h1').onclick = greet;
```

### 3. 🐘 PHP (Server-Side)
And-IDE includes a local PHP interpreter. Create a `test.php` file.
**Example Code:**
```php
<?php
  $name = "Developer";
  echo "<h1>Welcome, $name!</h1>";
  echo "Current Server Time: " . date("h:i:sa");
?>
```

### 🗄️ 4. SQL (Database)
Use the SQL module to manage data locally. You can execute queries directly:
**Example Commands:**
*   **Create Table:** `CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT);`
*   **Insert Data:** `INSERT INTO users (name) VALUES ('Jemmy');`
*   **View Data:** `SELECT * FROM users;`

---

## 🤖 AI Assistant (DDee)
If you get an error in your code, don't worry!
*   **Explain Code:** Highlight your code and ask DDee, "What is wrong with this PHP loop?"
*   **Generator:** Ask "Generate a responsive navigation bar using CSS Grid."

---

## 🔒 Security & Developer Protection
*   **Privacy:** All sensitive API keys and Firebase configurations are **hidden** from the public repository using `.gitignore`.
*   **Ownership:** This code is the intellectual property of **Jemmy09**. External developers can view the code but cannot modify your official release.

---

## 👋 About the Creator
**Jemmy Francisco** (Jemmy09)
Building tools to make mobile development accessible to everyone.

**Happy Coding!** 🚀
