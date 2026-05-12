<p align="center">
  <img src="https://raw.githubusercontent.com/Jemmy09/And-IDE/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="128" height="128" alt="And-IDE Logo">
</p>

<h1 align="center">And-IDE</h1>

<p align="center">
  <i>A humble, mobile-first workspace for web development on the go.</i>
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

Hi! Welcome to **And-IDE**. This is a project I've been working on to make mobile web development feel a bit more natural. The goal was simple: create a workspace that lets you write, test, and manage code directly from your phone without the usual friction.

Whether you're fixing a bug on the bus or just want to try out a new idea while away from your computer, And-IDE is here to help you stay in the flow.

---

## 🛠 What's inside?

I've tried to include the essentials you need to stay productive without the desktop:

### 1. The Multi-Tab Editor
No more switching back and forth between files. Open multiple tabs just like you would on a computer.
*   **Syntax Highlighting:** Makes your code easy to read and navigate.
*   **Language Support:** Works with the core building blocks of the web (HTML, CSS, JS, PHP).

### 2. DDe - Your Coding Companion
DDe is a built-in assistant powered by AI. 
*   **Stuck on a bug?** Ask DDe to help you find it.
*   **Need an explanation?** DDe can walk you through how a specific piece of code works.
*   **Feeling stuck?** Brainstorm new ideas or layouts directly in the chat.

### 3. Database Manager
Managing data on a phone is usually a headache. I've added a simple SQL interface so you can:
*   View your tables and data.
*   Run custom queries.
*   Keep your project's backend organized without leaving the app.

### 4. Search & Dashboard
*   **Global Search:** Find any variable, tag, or string across your entire project instantly.
*   **Project Stats:** See your total file count, total lines written, and how much storage you're using at a glance.

### 5. Quick Templates
Start faster with pre-written snippets for common tasks like:
*   User login forms.
*   Database connections.
*   Portfolio structures.

---

## 🚀 Getting Started

Follow these steps to get the app running on your own device:

### Prerequisites
*   **Android Studio** (the latest version is recommended).
*   An Android device or emulator.
*   **API Keys:** For DDe (the AI assistant) to work, you'll need an API key from [Google Gemini](https://ai.google.dev/) or [Groq](https://console.groq.com/keys).

### Installation
1.  **Clone the project:** 
    ```bash
    git clone https://github.com/Jemmy09/And-IDE.git
    ```
2.  **Open in Android Studio:** 
    Wait for the Gradle sync to finish so all dependencies are ready.
3.  **Add your keys:**
    Open the `local.properties` file in your project's root folder and add these lines:
    ```properties
    gemini.api.key=YOUR_GEMINI_KEY
    groq.api.key=YOUR_GROQ_KEY
    ```
4.  **Run it:** 
    Press the green **Run** button in Android Studio to install the app on your device.

---

## 💡 Using the app

*   **Creating Files:** Use the dashboard to start a new project or create individual files.
*   **Editing:** Tap a file to open it. You can open several files at once and switch between them using the tabs at the top.
*   **Asking DDe:** Tap the chat icon to talk to DDe. You can paste code there and ask for advice or troubleshooting help.
*   **Managing Data:** Head over to the Database section to interact with your project's local storage using SQL.

---

## 👋 Why I built this
I wanted a tool that didn't feel clunky or overloaded. Coding on a phone has its challenges, so I focused on making the most important features easy to reach. It’s built for people who want to keep building, even when they’re away from their main setup.

## 🤝 Want to help?
If you find a bug or have an idea for a feature that would make coding on mobile even better, I'd love to hear from you! 
*   Feel free to **open an issue**.
*   **Pull requests** are always welcome if you'd like to contribute code.

Thanks for checking out And-IDE! I hope it helps you bring your ideas to life.

**Happy Coding!**
