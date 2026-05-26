# Code Studio 2.0
Code Studio 2.0 is a powerful mobile Integrated Development Environment (IDE) designed for Android, bringing a professional desktop coding experience straight to your mobile device. Built with Jetpack Compose and Kotlin, this application combines a high-quality code editor with essential development tools like GitHub and Gemini AI.
## ✨ Key Features
 * **Professional Code Editor:** Syntax highlighting for multiple programming languages, line numbers, customizable font sizes, and UI themes (Dark/Light).
 * **GitHub Integration:** Connect directly to your GitHub account using a Personal Access Token. Support for searching repositories, browsing files, viewing commit history, and creating new commits right from your phone.
 * **Gemini AI - Smart Assistant:** Integrated Gemini API helps explain code, generate boilerplate code, debug errors, and provide intelligent programming suggestions.
 * **Local Workspace:** Manage projects and files with ease. Initialize, edit, and store files directly on your device (utilizing a local Room database).
 * **Profile & Personal Security:**
   * Set up your display name and user identity.
   * **Secure App Lock:** Protect your workspace using a 4-to-6-digit PIN.
   * **Biometrics:** Support for quick unlocking via Fingerprint or Face ID (if supported by the device).
 * **Comprehensive Backup & Restore:** Export and import your entire configuration, tokens, personal profiles, and app settings via JSON format. Easily migrate your setup between devices.
 * **Code Snippets Management:** Save and reuse frequently used code blocks.
 * **Code Diff Tool:** An intuitive tool to compare differences between code versions.
 * **Multi-language Support:** UI available in both Vietnamese and English.
## 🚀 Initial Setup Guide
 1. **First Launch:** When opening the app for the first time, you will be greeted by the profile setup screen.
 2. **Configuration:** Enter your Display Name (Optional: Enter your GitHub Username and Token to automatically sync your repositories).
 3. **Default Workspace:** The app will automatically create a personalized Workspace under your name to help you get started.
 4. **Enable Security Lock:** Navigate to the "Advanced & Integrations" dropdown menu within Settings, toggle "App Lock with PIN", and enter a PIN to protect your data.
## 💻 Tech Stack
 * **Platform:** Android (Kotlin).
 * **UI Framework:** Jetpack Compose (Material Design 3).
 * **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture.
 * **Database:** Room Database (For offline, local storage of files, snippets, and workspaces).
 * **Networking & APIs:** Retrofit 2 + OkHttp (For connecting to the GitHub API & Google Gemini API).
 * **Security:** BiometricPrompt (AndroidX Biometrics) & SharedPreferences.
 * **Build Automation:** Gradle Kotlin DSL.
## 🔒 Token Security
Your GitHub Personal Access Token and Gemini API Key are securely encrypted on your device and are strictly used for direct API calls between your device and the GitHub/Google servers. The application does not collect or share any of your personal tokens.
## 🤝 Contributing
Have a great idea or want to improve Code Studio? Feel free to open an Issue or submit a Pull Request (PR).
