package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import com.example.ui.components.ThemeRegistry
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiRetrofitClient
import com.example.api.GithubContentItem
import com.example.api.GithubRepoItem
import com.example.api.GithubRetrofitClient
import com.example.database.AppDatabase
import com.example.database.EditorRepository
import com.example.database.FileEntity
import com.example.database.ProjectEntity
import com.example.database.SnippetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SidebarTab {
    FILES, GITHUB, GEMINI, PREVIEW, SNIPPETS, OUTLINE, DIFF, SETTINGS
}

data class ChatMessage(
    val sender: String, // "user" or "gemini"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EditorRepository
    val allProjects: StateFlow<List<ProjectEntity>>
    val allSnippets: StateFlow<List<SnippetEntity>>

    // Sidebar navigation State
    var activeTab by mutableStateOf(SidebarTab.FILES)

    // Workspace Project State
    var currentProject by mutableStateOf<ProjectEntity?>(null)
    private val _projectFiles = MutableStateFlow<List<FileEntity>>(emptyList())
    val projectFiles: StateFlow<List<FileEntity>> = _projectFiles.asStateFlow()
    private var projectFilesJob: kotlinx.coroutines.Job? = null

    // Editor Area State
    var activeFile by mutableStateOf<FileEntity?>(null)
    var editorText by mutableStateOf("")
    var editorLanguage by mutableStateOf("kotlin")

    // Gemini Assistant State
    private val _geminiChat = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("system", "Chào bạn! Tôi là trợ lý AI chuyên gia của Code Studio, được phát triển bởi tác giả Phúc Nguyễn. Tôi có thể hỗ trợ giải thích code, viết code, sửa lỗi, và gợi ý tối ưu thuật toán. Hãy hỏi bất cứ điều gì!")
    ))
    val geminiChat: StateFlow<List<ChatMessage>> = _geminiChat.asStateFlow()
    var geminiPrompt by mutableStateOf("")
    var isGeminiGenerating by mutableStateOf(false)
    val activeAiModel = "gemini-3.5-flash"

    // AI Auto-Coder direct-writing values
    var aiAutoCoderPrompt by mutableStateOf("")
    var isAiAutoCoderGenerating by mutableStateOf(false)

    // GitHub API Integration State
    var githubQuery by mutableStateOf("")
    var isSearchingGithub by mutableStateOf(false)
    var githubQueryError by mutableStateOf<String?>(null)
    private val _githubRepos = MutableStateFlow<List<GithubRepoItem>>(emptyList())
    val githubRepos: StateFlow<List<GithubRepoItem>> = _githubRepos.asStateFlow()

    // Inside Github Repo explorer State
    var exploredRepo by mutableStateOf<GithubRepoItem?>(null)
    var exploredRepoPath by mutableStateOf("")
    private val _exploredItems = MutableStateFlow<List<GithubContentItem>>(emptyList())
    val exploredItems: StateFlow<List<GithubContentItem>> = _exploredItems.asStateFlow()
    var isLoadingExploredItems by mutableStateOf(false)
    var isShowingRepoCommits by mutableStateOf(false)
    private val _githubPullRequests = MutableStateFlow<List<com.example.api.GithubPullRequest>>(emptyList())
    val githubPullRequests: StateFlow<List<com.example.api.GithubPullRequest>> = _githubPullRequests.asStateFlow()
    var isLoadingPullRequests by mutableStateOf(false)

    var currentRepoDetail by mutableStateOf<com.example.api.GithubRepoDetail?>(null)
    var isLoadingRepoDetail by mutableStateOf(false)
    var isCloningRepo by mutableStateOf(false)

    var aiProvider by mutableStateOf("gemini") // "gemini" or "copilot"

    private val _trendingRepos = MutableStateFlow<List<com.example.api.GithubRepoItem>>(emptyList())
    val trendingRepos: StateFlow<List<com.example.api.GithubRepoItem>> = _trendingRepos.asStateFlow()
    var isLoadingTrending by mutableStateOf(false)

    private val _githubCommits = MutableStateFlow<List<com.example.api.GithubCommitItem>>(emptyList())
    val githubCommits: StateFlow<List<com.example.api.GithubCommitItem>> = _githubCommits.asStateFlow()
    var isLoadingCommits by mutableStateOf(false)

    fun cloneRepoToWorkspace(repo: GithubRepoItem) {
        val detail = currentRepoDetail
        if (detail == null) {
            isCloningRepo = true
            viewModelScope.launch {
                try {
                    val authHeader = if (githubToken.isNotEmpty()) "Bearer $githubToken" else null
                    val d = withContext(Dispatchers.IO) {
                        GithubRetrofitClient.service.getRepoDetail(owner = repo.owner.login, repo = repo.name, token = authHeader)
                    }
                    performClone(repo, d.defaultBranch)
                } catch(e: Exception) {
                    isCloningRepo = false
                }
            }
        } else {
            performClone(repo, detail.defaultBranch)
        }
    }

    private fun performClone(repo: GithubRepoItem, branch: String) {
        isCloningRepo = true
        viewModelScope.launch {
            try {
                // create a new project
                val newProj = ProjectEntity(
                    name = repo.name,
                    description = repo.description ?: "",
                    githubUsername = repo.owner.login,
                    githubToken = githubToken
                )
                val newProjId = repository.insertProject(newProj)

                // Fetch zipball
                val authHeader = if (githubToken.isNotEmpty()) "Bearer $githubToken" else null
                val responseBody = withContext(Dispatchers.IO) {
                    GithubRetrofitClient.service.getZipArchive(repo.owner.login, repo.name, branch, authHeader)
                }

                withContext(Dispatchers.IO) {
                    java.util.zip.ZipInputStream(responseBody.byteStream()).use { zis ->
                        var entry = zis.nextEntry
                        while(entry != null) {
                            if (!entry.isDirectory) {
                                // GitHub zipball includes root folder like owner-repo-sha/
                                val nameParts = entry.name.split("/")
                                val cleanPath = if (nameParts.size > 1) {
                                    nameParts.drop(1).joinToString("/")
                                } else {
                                    entry.name
                                }

                                val contentBytes = zis.readBytes()
                                val content = contentBytes.toString(Charsets.UTF_8)
                                val name = cleanPath.substringAfterLast('/')

                                val f = FileEntity(
                                    projectId = newProjId,
                                    name = name,
                                    path = cleanPath,
                                    isFolder = false,
                                    content = content,
                                    language = detectLanguage(name)
                                )
                                repository.insertFile(f)
                            }
                            entry = zis.nextEntry
                        }
                    }
                }
                val proj = repository.allProjects.first().find { it.id == newProjId }
                if (proj != null) {
                    selectProject(proj)
                }
                activeTab = SidebarTab.FILES
            } catch(e: Exception) {
               // Ignore
            } finally {
                isCloningRepo = false
            }
        }
    }

    // User settings
    var githubToken by mutableStateOf("")
    var githubUsername by mutableStateOf("")
    var geminiApiKey by mutableStateOf("")
    var editorTheme by mutableStateOf("Github Dark")
    var fontSize by mutableStateOf(12)
    var runAutoHighlight by mutableStateOf(true)
    var isVietnamese by mutableStateOf(true)

    // Profile & Security settings
    var isFirstLaunch by mutableStateOf(true)
    var profileName by mutableStateOf("")
    var hasAppLock by mutableStateOf(false)
    var appLockPin by mutableStateOf("")
    var isAppUnlocked by mutableStateOf(false)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = EditorRepository(database.workspaceDao())
        allProjects = repository.allProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        allSnippets = repository.allSnippets.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Read personal settings from SharedPreferences
        val prefs = application.getSharedPreferences("monaco_prefs", Context.MODE_PRIVATE)
        githubToken = prefs.getString("github_token", "") ?: ""
        githubUsername = prefs.getString("github_username", "") ?: ""
        geminiApiKey = prefs.getString("gemini_api_key", "") ?: ""
        editorTheme = prefs.getString("editor_theme", "Github Dark") ?: "Github Dark"
        fontSize = prefs.getInt("editor_font_size", 12)
        runAutoHighlight = prefs.getBoolean("auto_highlight", true)
        isVietnamese = prefs.getBoolean("is_vietnamese", true)

        _geminiChat.value = listOf(
            ChatMessage("system", if (isVietnamese) "Chào bạn! Tôi là trợ lý AI chuyên gia của Code Studio, được phát triển bởi tác giả Phúc Nguyễn. Tôi có thể hỗ trợ giải thích code, viết code, sửa lỗi, và gợi ý tối ưu thuật toán. Hãy hỏi bất cứ điều gì!" else "Hello! I am Code Studio's expert AI assistant, developed by author Phuc Nguyen. I can help explain, write, debug code, and optimize algorithms. Ask me anything!")
        )

        isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        profileName = prefs.getString("profile_name", "") ?: ""
        hasAppLock = prefs.getBoolean("has_app_lock", false)
        appLockPin = prefs.getString("app_lock_pin", "") ?: ""
        isAppUnlocked = !hasAppLock

        viewModelScope.launch {
            // Seed a default workspace if none exists
            val projectsList = repository.allProjects.first()
            if (projectsList.isEmpty()) {
                seedDefaultWorkspace()
            } else {
                val demoProj = projectsList.find { it.name == "Project Demo Code Studio" }
                if (demoProj != null) {
                    updateDemoWorkspaceFiles(demoProj.id)
                }
                selectProject(projectsList.first())
            }
        }
    }

    private suspend fun updateDemoWorkspaceFiles(projId: Long) {
        val files = repository.getFilesForProject(projId).first()
        val newestContent = mapOf(
            "README.md" to """# 🚀 CHÀO MỪNG ĐẾN VỚI CODE STUDIO | WELCOME TO CODE STUDIO

**Code Studio** là một môi trường phát triển tích hợp (IDE) di động đỉnh cao dành cho Android, được thiết kế và tối ưu hóa tối đa nhằm giúp các lập trình viên viết mã, thử nghiệm và đồng bộ hóa mọi lúc, mọi nơi. 

**Code Studio** is an elite, offline-first mobile Integrated Development Environment (IDE) built for Android, meticulously engineered to enable developers to code, test, and synchronize effortlessly on the go.

---

## 🌟 CÁC TÍNH NĂNG NỔI BẬT | HIGHLIGHTED FEATURES

### 1. 📂 Quản Lý Không Gian Làm Việc | Dynamic Workspace Management
*   **Tiếng Việt**: Tổ chức mã nguồn của bạn thành các Workspace độc lập. Tạo mới, đổi tên hoặc xóa các thư mục và tệp tin nhanh chóng chỉ với vài lần chạm.
*   **English**: Organize your source code into isolated Projects. Drag, expand, create, rename, or delete nested directories and files seamlessly.

### 2. ✍️ Trình Soạn Thảo Chuyên Nghiệp | Premium Monaco Text Editor UI
*   **Tiếng Việt**: Tích hợp nhân soạn thảo Monaco của Microsoft với nhiều bộ chủ đề (Theme) cực đẹp như Dark, Dracul, Midnight, Github Chrome... Hỗ trợ tự động hoàn thành cú pháp, số dòng, tùy chỉnh kích thước chữ, và tự động lưu.
*   **English**: Powered by Microsoft's Monaco editor engine. Supports beautiful premium themes (dark/light), custom font-size, auto-saving logic, auto-syntax highlighting, and line numbers.

### 3. 🔍 Tìm Kiếm Cấu Trúc Mã Nguồn | Double-Tap Outline & Symbol Seek
*   **Tiếng Việt**: Sử dụng thanh cấu trúc (Outline panel) để nhận diện nhanh các lớp (Class), hàm (Function) hay thuộc tính. Bạn có thể nhấn đúp vào ký hiệu để con trỏ soạn thảo nhảy trực tiếp tới dòng khoa báo tương ứng.
*   **English**: Use the dedicated Symbols Column to detect classes, variables, and methods inside your files. Double-click any symbol to instantly snap the editor's cursor to the corresponding line.

### 4. ⚡ Chạy Thử Web Tức Thì | HTML/CSS/JS Instant Preview
*   **Tiếng Việt**: Trình xem trước Web thời gian thực xử lý mượt mà toàn bộ liên kết nội bộ giữa các file HTML, CSS và JavaScript. Bạn có thể thay đổi giao diện và quan sát chuyển động UI phản hồi trực quan ngay lập tức.
*   **English**: Real-time sandboxed Web viewer resolves external linked stylesheets and script files seamlessly. Modify code on-the-fly and watch your layout render and respond instantly.

### 5. 🤖 Trợ Lý Trí Tuệ Nhân Tạo | Gemini AI Developer Expert
*   **Tiếng Việt**: Trò chuyện trực tiếp cùng trợ lý lập trình chuyên sâu của Google Gemini. Tối ưu thuật toán, phát hiện lỗ hổng bảo mật, giải thích mã phức tạp hay thậm chí phục dựng tính năng mới an toàn ngay trên thiết bị.
*   **English**: Chat, debug, and refactor directly with Google’s Gemini AI integrated model. Let the assistant analyze performance bottlenecks, translate structures, or write clean code easily.

### 6. 🐙 Đồng Bộ Hóa GitHub | Two-Way GitHub Repository Sync
*   **Tiếng Việt**: Kết nối trực tiếp tài khoản cá nhân thông qua GitHub Access Token. Bạn có thể nhân bản (Clone) kho chứa trực tuyến, theo dõi thay đổi trực quan (Diff panel), cập nhật dữ liệu (Git Pull) và lưu trữ trực tiếp (Git Push) mã nguồn phiên bản mới nhất.
*   **English**: Connect securely using your GitHub Access Token. Clone repositories, preview file changes side-by-side inside the beautiful Diff interface, pull updates, and push your commits directly to remote origins.

---

## 🛠️ HƯỚNG DẪN BẮT ĐẦU | QUICK START GUIDE

1.  📂 **Mở tệp `index.html`** trong Sidebar bên trái và bắt đầu tinh chỉnh nội dung.
2.  🎨 **Tùy biến `css/style.css`** để biến đổi phong cách thiết kế, phông nền hay màu chuyển sắc.
3.  ⚙️ **Truy cập Cài đặt (Settings)** ở góc dưới để kích hoạt Chế độ tiếng Việt / tiếng Anh, hoặc chuyển đổi giao diện sáng/tối.
4.  🔒 **Mã bảo mật PIN**: Thiết lập mã PIN khóa màn hình trong mục Bảo mật để mã hóa không gian làm việc cục bộ của bạn an toàn nhất.

*Chúc bạn có những trải nghiệm thăng hoa và sáng tạo cùng Code Studio!*
*Have a wonderful journey pushing the boundaries of coding with Code Studio!*

---
## 📜 BẢN QUYỀN & GIẤY PHÉP | LICENSE & CREDITS
*   **Tác giả (Author)**: Nguyễn Hồng Diễm Phúc 🇻🇳
*   **Giấy phép (License)**: MIT License (2026)
""",
            "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Code Studio - Premium Mobile IDE</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <!-- Language Selector Floating Header -->
    <header class="header-nav">
        <div class="logo-wrapper">
            <svg class="glowing-logo" viewBox="0 0 100 100" width="36" height="36">
                <defs>
                    <linearGradient id="logo-grad" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stop-color="#388bfd" />
                        <stop offset="100%" stop-color="#a371f7" />
                    </linearGradient>
                    <filter id="glow">
                        <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
                        <feMerge>
                            <feMergeNode in="coloredBlur"/>
                            <feMergeNode in="SourceGraphic"/>
                        </feMerge>
                    </filter>
                </defs>
                <rect x="15" y="15" width="70" height="70" rx="18" fill="url(#logo-grad)" filter="url(#glow)"/>
                <path d="M40 38 L30 50 L40 62 M60 38 L70 50 L60 62" stroke="white" stroke-width="6" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
                <rect x="46" y="56" width="10" height="4" fill="white" rx="1"/>
            </svg>
            <span class="app-brand-name">Code Studio</span>
        </div>
        <div class="lang-controls">
            <button class="lang-btn active" id="btn-lang-en">EN</button>
            <span class="lang-divider">|</span>
            <button class="lang-btn" id="btn-lang-vi">VN</button>
        </div>
    </header>

    <main class="container">
        <!-- Hero Section -->
        <section class="hero-section">
            <h1 class="hero-title" id="txt-hero-title">Elevate Coding Everywhere</h1>
            <p class="hero-subtitle" id="txt-hero-subtitle">The ultimate offline-first mobile IDE featuring Monaco editor power, GitHub integration, and specialized Gemini AI assistance.</p>
            <div class="hero-badge" id="txt-hero-badge">⚡ Crafted by Nguyen Hong Diem Phuc</div>
        </section>

        <!-- Dynamic Core Feature Preview Card -->
        <section class="preview-card">
            <div class="card-glow"></div>
            <div class="card-interior">
                <div class="terminal-bar">
                    <span class="dot red"></span>
                    <span class="dot yellow"></span>
                    <span class="dot green"></span>
                    <span class="window-title" id="txt-window-title">code_studio_preview.js</span>
                </div>
                
                <div class="card-body">
                    <h2 class="card-title" id="txt-card-title">Experience the Fluidity</h2>
                    <p class="card-desc" id="txt-card-desc">Click the buttons below to switch themes or test interactive messages in our live playground console.</p>
                    
                    <div class="playground-console">
                        <div class="console-overlay"></div>
                        <div class="console-header">
                            <span class="status-dot pulsing"></span>
                            <span class="console-label">PLAYGROUND INPUT / OUTPUT</span>
                        </div>
                        <div class="console-output" id="output-box">
                            System initialized. Welcome to Code Studio!
                        </div>
                    </div>

                    <div class="control-actions">
                        <button class="action-btn btn-primary" id="btn-theme-cycle">
                            <span class="icon">🎨</span>
                            <span id="txt-btn-theme">Cycle Theme</span>
                        </button>
                        <button class="action-btn btn-secondary" id="btn-msg-cycle">
                            <span class="icon">🤖</span>
                            <span id="txt-btn-msg">Next Quote</span>
                        </button>
                    </div>
                </div>
            </div>
        </section>

        <!-- Features Showcase Grid -->
        <section class="features-grid">
            <div class="feature-box">
                <div class="feature-icon-wrap">💻</div>
                <h3 id="feat-title-1">Premium Editor</h3>
                <p id="feat-desc-1">Powered by Monaco engine, custom font-sizing, and double-tap symbol structure navigation.</p>
            </div>
            <div class="feature-box">
                <div class="feature-icon-wrap">⚡</div>
                <h3 id="feat-title-2">Live Web Preview</h3>
                <p id="feat-desc-2">Run HTML, CSS, JavaScript instantaneously without external server overheads.</p>
            </div>
            <div class="feature-box">
                <div class="feature-icon-wrap">🤖</div>
                <h3 id="feat-title-3">Gemini Expert AI</h3>
                <p id="feat-desc-3">Refactor structures, explain logic, and debug errors safely inside your view.</p>
            </div>
            <div class="feature-box">
                <div class="feature-icon-wrap">🐙</div>
                <h3 id="feat-title-4">Git Repository Synchronization</h3>
                <p id="feat-desc-4">Clone, pull, diff, commit and push modifications directly to GitHub origins.</p>
            </div>
        </section>
    </main>

    <footer class="footer">
        <p class="footer-text">
            © 2026 Nguyễn Hồng Diễm Phúc. All rights reserved. Registered under MIT License.
        </p>
    </footer>

    <script src="js/script.js"></script>
</body>
</html>
""",
            "style.css" to """:root {
    --bg-primary: #0b0f19;
    --card-bg: rgba(17, 24, 39, 0.7);
    --text-primary: #f3f4f6;
    --text-secondary: #9ca3af;
    --accent: #388bfd;
    --accent-glow: rgba(56, 139, 253, 0.4);
    --border-color: rgba(255, 255, 255, 0.08);
}

* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
    background-color: var(--bg-primary);
    background-image: 
        radial-gradient(circle at 10% 20%, rgba(56, 139, 253, 0.15) 0%, transparent 40%),
        radial-gradient(circle at 90% 80%, rgba(163, 113, 247, 0.15) 0%, transparent 40%);
    background-attachment: fixed;
    color: var(--text-primary);
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-bottom: 40px;
}

/* Header & Language controls */
.header-nav {
    width: 100%;
    max-width: 1100px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 24px;
    border-bottom: 1px solid var(--border-color);
    backdrop-filter: blur(8px);
    margin-bottom: 20px;
}

.logo-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
}

.glowing-logo {
    filter: drop-shadow(0px 0px 8px var(--accent-glow));
    transition: transform 0.5s ease;
}

.glowing-logo:hover {
    transform: rotate(15deg) scale(1.1);
}

.app-brand-name {
    font-size: 1.3rem;
    font-weight: 800;
    background: linear-gradient(135deg, #4f9cfd, #c084fc);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    letter-spacing: -0.5px;
}

.lang-controls {
    display: flex;
    align-items: center;
    gap: 8px;
    background: rgba(255, 255, 255, 0.04);
    padding: 4px 12px;
    border-radius: 99px;
    border: 1px solid var(--border-color);
}

.lang-btn {
    background: none;
    border: none;
    color: var(--text-secondary);
    font-weight: 700;
    font-size: 0.85rem;
    cursor: pointer;
    transition: color 0.3s, transform 0.2s;
    padding: 4px 8px;
}

.lang-btn.active {
    color: #4f9cfd;
    text-shadow: 0 0 8px rgba(79, 156, 253, 0.5);
}

.lang-btn:hover {
    color: var(--text-primary);
    transform: scale(1.05);
}

.lang-divider {
    color: rgba(255, 255, 255, 0.2);
    font-size: 0.8rem;
    user-select: none;
}

/* Container */
.container {
    width: 100%;
    max-width: 900px;
    padding: 0 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    flex: 1;
}

/* Hero Section */
.hero-section {
    text-align: center;
    margin: 30px 0 40px 0;
}

.hero-title {
    font-size: 2.8rem;
    font-weight: 900;
    letter-spacing: -1.5px;
    line-height: 1.15;
    background: linear-gradient(to right, #ffffff, #94a3b8);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    margin-bottom: 16px;
    animation: fadeIn 1s ease-out;
}

.hero-subtitle {
    font-size: 1.1rem;
    color: var(--text-secondary);
    line-height: 1.6;
    max-width: 650px;
    margin: 0 auto 20px auto;
    animation: fadeIn 1.2s ease-out;
}

.hero-badge {
    display: inline-block;
    background: rgba(163, 113, 247, 0.15);
    color: #c084fc;
    border: 1px solid rgba(163, 113, 247, 0.3);
    padding: 6px 14px;
    border-radius: 99px;
    font-size: 0.8rem;
    font-weight: 700;
    letter-spacing: 0.5px;
    text-transform: uppercase;
}

/* Card Section */
.preview-card {
    position: relative;
    width: 100%;
    border-radius: 20px;
    overflow: hidden;
    margin-bottom: 50px;
}

.card-glow {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, rgba(88, 166, 255, 0.1), rgba(163, 113, 247, 0.1));
    border-radius: 20px;
    z-index: 1;
    pointer-events: none;
}

.card-interior {
    background: var(--card-bg);
    border: 1px solid var(--border-color);
    border-radius: 20px;
    backdrop-filter: blur(20px);
    overflow: hidden;
    position: relative;
    z-index: 2;
}

.terminal-bar {
    background: rgba(15, 23, 42, 0.8);
    padding: 12px 18px;
    display: flex;
    align-items: center;
    border-bottom: 1px solid var(--border-color);
}

.dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    margin-right: 6px;
}

.dot.red { background-color: #ff5f56; }
.dot.yellow { background-color: #ffbd2e; }
.dot.green { background-color: #27c93f; }

.window-title {
    font-family: monospace;
    font-size: 0.8rem;
    color: var(--text-secondary);
    margin-left: 12px;
}

.card-body {
    padding: 30px;
}

.card-title {
    font-size: 1.5rem;
    font-weight: 800;
    margin-bottom: 8px;
    letter-spacing: -0.5px;
}

.card-desc {
    color: var(--text-secondary);
    font-size: 0.95rem;
    margin-bottom: 24px;
    line-height: 1.5;
}

/* Micro Console */
.playground-console {
    background: #060913;
    border-radius: 12px;
    border: 1px solid rgba(255, 255, 255, 0.05);
    padding: 18px;
    min-height: 110px;
    position: relative;
    margin-bottom: 24px;
    overflow: hidden;
    display: flex;
    flex-direction: column;
}

.console-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
}

.status-dot {
    width: 8px;
    height: 8px;
    background-color: #4f9cfd;
    border-radius: 50%;
}

.status-dot.pulsing {
    box-shadow: 0 0 0 0 rgba(79, 156, 253, 0.7);
    animation: pulse 1.6s infinite;
}

.console-label {
    font-family: monospace;
    font-size: 0.7rem;
    color: #4f9cfd;
    letter-spacing: 1px;
    font-weight: bold;
}

.console-output {
    font-family: 'Consolas', 'Fira Code', monospace;
    font-size: 0.9rem;
    color: #a5b4fc;
    line-height: 1.5;
    flex: 1;
    word-break: break-word;
    transition: color 0.3s ease;
}

.console-overlay {
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    background: radial-gradient(circle at center, transparent 60%, rgba(0,0,0,0.3) 100%);
    pointer-events: none;
}

/* Buttons */
.control-actions {
    display: flex;
    gap: 14px;
    justify-content: center;
}

.action-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 24px;
    font-size: 0.9rem;
    font-weight: 700;
    border-radius: 10px;
    border: none;
    cursor: pointer;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.action-btn .icon {
    font-size: 1.1rem;
}

.action-btn.btn-primary {
    background: linear-gradient(135deg, #388bfd, #5c6bc0);
    color: white;
    box-shadow: 0 4px 15px rgba(56, 139, 253, 0.25);
}

.action-btn.btn-primary:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(56, 139, 253, 0.4);
}

.action-btn.btn-secondary {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid var(--border-color);
    color: var(--text-primary);
}

.action-btn.btn-secondary:hover {
    background: rgba(255, 255, 255, 0.1);
    transform: translateY(-2px);
}

/* Feature Showcase Grid */
.features-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    width: 100%;
}

.feature-box {
    background: rgba(255,255,255,0.02);
    border: 1px solid var(--border-color);
    border-radius: 16px;
    padding: 24px;
    backdrop-filter: blur(10px);
    transition: all 0.3s ease;
}

.feature-box:hover {
    background: rgba(255,255,255,0.04);
    border-color: rgba(56, 139, 253, 0.2);
    transform: translateY(-3px);
}

.feature-icon-wrap {
    font-size: 1.8rem;
    margin-bottom: 12px;
}

.feature-box h3 {
    font-size: 1.1rem;
    font-weight: 700;
    margin-bottom: 8px;
    letter-spacing: -0.2px;
}

.feature-box p {
    font-size: 0.85rem;
    color: var(--text-secondary);
    line-height: 1.5;
}

/* Footer style */
.footer {
    width: 100%;
    text-align: center;
    padding: 40px 20px 0 20px;
    border-top: 1px solid var(--border-color);
    margin-top: 50px;
}

.footer-text {
    font-size: 0.8rem;
    color: #4b5563;
    line-height: 1.5;
    font-family: monospace;
}

/* Animations */
@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}

@keyframes pulse {
    0% {
        transform: scale(0.95);
        box-shadow: 0 0 0 0 rgba(79, 156, 253, 0.7);
    }
    70% {
        transform: scale(1);
        box-shadow: 0 0 0 6px rgba(79, 156, 253, 0);
    }
    100% {
        transform: scale(0.95);
        box-shadow: 0 0 0 0 rgba(79, 156, 253, 0);
    }
}

/* Responsive adjustment */
@media(max-width: 600px) {
    .features-grid {
        grid-template-columns: 1fr;
    }
    .hero-title {
        font-size: 2rem;
    }
    .hero-subtitle {
        font-size: 0.95rem;
    }
}
""",
            "script.js" to """document.addEventListener("DOMContentLoaded", () => {
    // Buttons & Outputs
    const btnThemeCycle = document.getElementById("btn-theme-cycle");
    const btnMsgCycle = document.getElementById("btn-msg-cycle");
    const outputBox = document.getElementById("output-box");

    // Language Toggle Buttons
    const btnLangEn = document.getElementById("btn-lang-en");
    const btnLangVi = document.getElementById("btn-lang-vi");

    // Bilingual Elements & Translations
    const elementsToTranslate = {
        "txt-hero-title": {
            en: "Elevate Coding Everywhere",
            vi: "Nâng Tầm Lập Trình Mọi Nơi"
        },
        "txt-hero-subtitle": {
            en: "The ultimate offline-first mobile IDE featuring Monaco editor power, GitHub integration, and specialized Gemini AI assistance.",
            vi: "Môi trường lập trình di động offline-first tối tân tích hợp sức mạnh nhân Monaco, đồng bộ GitHub và Trợ lý Chuyên gia Gemini AI."
        },
        "txt-hero-badge": {
            en: "⚡ Crafted by Nguyen Hong Diem Phuc",
            vi: "⚡ Thiết kế bởi Nguyễn Hồng Diễm Phúc"
        },
        "txt-window-title": {
            en: "code_studio_preview.js",
            vi: "code_studio_ban_nhap.js"
        },
        "txt-card-title": {
            en: "Experience the Fluidity",
            vi: "Trải Nghiệm Sự Mượt Mà"
        },
        "txt-card-desc": {
            en: "Click the buttons below to switch themes or test interactive messages in our live playground console.",
            vi: "Bấm nút bên dưới để chuyển đổi màu nền hoặc chạy thử thông điệp trong bảng trải nghiệm trực quan."
        },
        "txt-btn-theme": {
            en: "Cycle Theme",
            vi: "Thay Màu Nền"
        },
        "txt-btn-msg": {
            en: "Next Quote",
            vi: "Thông Điệp Mới"
        },
        "feat-title-1": {
            en: "Premium Editor",
            vi: "Soạn Thảo Đẳng Cấp"
        },
        "feat-desc-1": {
            en: "Powered by Monaco engine, custom font-sizing, and double-tap symbol structure navigation.",
            vi: "Hỗ trợ nhân Monaco cao cấp, đổi cỡ chữ linh hoạt, và chạm đúp định vị cấu hình tệp nhanh."
        },
        "feat-title-2": {
            en: "Live Web Preview",
            vi: "Trình Xem Trước Sống Động"
        },
        "feat-desc-2": {
            en: "Run HTML, CSS, JavaScript instantaneously without external server overheads.",
            vi: "Chạy thử các tệp HTML, CSS, JavaScript lập tức mà không cần máy chủ trung gian."
        },
        "feat-title-3": {
            en: "Gemini Expert AI",
            vi: "Chuyên Gia Gemini AI"
        },
        "feat-desc-3": {
            en: "Refactor structures, explain logic, and debug errors safely inside your view.",
            vi: "Tối ưu hóa mã nguồn, giải thích cách thức hoạt động và dò lỗi thông minh trực quan."
        },
        "feat-title-4": {
            en: "Git Synchronization",
            vi: "Đồng Bộ Hóa Git"
        },
        "feat-desc-4": {
            en: "Clone, pull, diff, commit and push modifications directly to GitHub origins.",
            vi: "Sao chép, theo dõi nhánh thay đổi, lưu trữ và đẩy mã trực tiếp lên kho lưu trữ GitHub."
        }
    };

    // Messages
    const quotes = {
        en: [
            "Ready to write beautiful code on your mobile device!",
            "Double click or tap Outline Symbols to navigate your declarations instantly.",
            "Set your private PIN access token inside Security to restrict folder access.",
            "Your credentials keys are absolute safe and localized offline on Room DB.",
            "We make programming accessible anytime, anywhere! 🚀"
        ],
        vi: [
            "Sẵn sàng viết code cực đẹp ngay trên thiết bị di động của bạn!",
            "Nhấp đúp hoặc chạm vào Cây Kí Hiệu để đi tới dòng khai báo ngay lập tức.",
            "Cài đặt khóa PIN bảo mật an toàn chống mọi sự xâm nhập phi pháp.",
            "Khóa API Gemini và Token GitHub được lưu cục bộ an toàn tuyệt đối.",
            "Đưa lập trình tiếp cận mọi người, mọi lúc, mọi nơi! 🚀"
        ]
    };

    const gradientThemes = [
        {
            bg: "radial-gradient(circle at 10% 20%, rgba(56, 139, 253, 0.15) 0%, transparent 40%), radial-gradient(circle at 90% 80%, rgba(163, 113, 247, 0.15) 0%, transparent 40%)",
            color: "#0b0f19",
            msg: { en: "Switched to Deep Cosmos theme.", vi: "Đã chuyển sang chủ đề Vũ Trụ Sâu." }
        },
        {
            bg: "radial-gradient(circle at 10% 20%, rgba(220, 38, 38, 0.15) 0%, transparent 45%), radial-gradient(circle at 90% 70%, rgba(245, 158, 11, 0.1) 0%, transparent 40%)",
            color: "#110707",
            msg: { en: "Switched to Crimson Nebula theme.", vi: "Đã chuyển sang chủ đề Tinh Vân Đỏ." }
        },
        {
            bg: "radial-gradient(circle at 10% 20%, rgba(16, 185, 129, 0.15) 0%, transparent 45%), radial-gradient(circle at 80% 80%, rgba(6, 182, 212, 0.15) 0%, transparent 45%)",
            color: "#040d0f",
            msg: { en: "Switched to Emerald Tech theme.", vi: "Đã chuyển sang chủ đề Công Nghệ Lục Bảo." }
        },
        {
            bg: "radial-gradient(circle at 15% 15%, rgba(236, 72, 153, 0.15) 0%, transparent 50%), radial-gradient(circle at 85% 85%, rgba(99, 102, 241, 0.15) 0%, transparent 50%)",
            color: "#0f0c1b",
            msg: { en: "Switched to Sunset Violet theme.", vi: "Đã chuyển sang chủ đề Hoàng Hôn Tím." }
        }
    ];

    let currentLang = "en";
    let themeIndex = 0;
    let quoteIndex = 0;

    function setLanguage(lang) {
        currentLang = lang;
        if (lang === "en") {
            btnLangEn.classList.add("active");
            btnLangVi.classList.remove("active");
            document.documentElement.lang = "en";
        } else {
            btnLangEn.classList.remove("active");
            btnLangVi.classList.add("active");
            document.documentElement.lang = "vi";
        }

        for (const [id, value] of Object.entries(elementsToTranslate)) {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = value[lang];
            }
        }
        
        outputBox.textContent = currentLang === "en" ? "Language switched to English!" : "Đã chuyển ngôn ngữ sang Tiếng Việt!";
    }

    btnLangEn.addEventListener("click", () => setLanguage("en"));
    btnLangVi.addEventListener("click", () => setLanguage("vi"));

    btnThemeCycle.addEventListener("click", () => {
        themeIndex = (themeIndex + 1) % gradientThemes.length;
        const selected = gradientThemes[themeIndex];
        
        document.body.style.backgroundColor = selected.color;
        document.body.style.backgroundImage = selected.bg;
        
        outputBox.textContent = selected.msg[currentLang];
        outputBox.style.color = "#a5b4fc";
    });

    btnMsgCycle.addEventListener("click", () => {
        const langQuotes = quotes[currentLang];
        quoteIndex = (quoteIndex + 1) % langQuotes.length;
        
        outputBox.textContent = langQuotes[quoteIndex];
        outputBox.style.color = "#388bfd";
    });

    setLanguage("vi");
});
"""
        )

        for (file in files) {
            val correctContent = newestContent[file.name]
            if (correctContent != null && file.content != correctContent) {
                repository.updateFile(file.copy(content = correctContent))
            }
        }
    }

    private suspend fun seedDefaultWorkspace() {
        val defaultProject = ProjectEntity(
            name = "Project Demo Code Studio",
            description = "Dự án mẫu khám phá các tính năng của Code Studio, HTML Preview và Gemini AI."
        )
        val projId = repository.insertProject(defaultProject)

        val filesToSeed = listOf(
            FileEntity(
                projectId = projId,
                name = "README.md",
                path = "README.md",
                isFolder = false,
                content = """# 🚀 CHÀO MỪNG ĐẾN VỚI CODE STUDIO | WELCOME TO CODE STUDIO

**Code Studio** là một môi trường phát triển tích hợp (IDE) di động đỉnh cao dành cho Android, được thiết kế và tối ưu hóa tối đa nhằm giúp các lập trình viên viết mã, thử nghiệm và đồng bộ hóa mọi lúc, mọi nơi. 

**Code Studio** is an elite, offline-first mobile Integrated Development Environment (IDE) built for Android, meticulously engineered to enable developers to code, test, and synchronize effortlessly on the go.

---

## 🌟 CÁC TÍNH NĂNG NỔI BẬT | HIGHLIGHTED FEATURES

### 1. 📂 Quản Lý Không Gian Làm Việc | Dynamic Workspace Management
*   **Tiếng Việt**: Tổ chức mã nguồn của bạn thành các Workspace độc lập. Tạo mới, đổi tên hoặc xóa các thư mục và tệp tin nhanh chóng chỉ với vài lần chạm.
*   **English**: Organize your source code into isolated Projects. Drag, expand, create, rename, or delete nested directories and files seamlessly.

### 2. ✍️ Trình Soạn Thảo Chuyên Nghiệp | Premium Monaco Text Editor UI
*   **Tiếng Việt**: Tích hợp nhân soạn thảo Monaco của Microsoft với nhiều bộ chủ đề (Theme) cực đẹp như Dark, Dracul, Midnight, Github Chrome... Hỗ trợ tự động hoàn thành cú pháp, số dòng, tùy chỉnh kích thước chữ, và tự động lưu.
*   **English**: Powered by Microsoft's Monaco editor engine. Supports beautiful premium themes (dark/light), custom font-size, auto-saving logic, auto-syntax highlighting, and line numbers.

### 3. 🔍 Tìm Kiếm Cấu Trúc Mã Nguồn | Double-Tap Outline & Symbol Seek
*   **Tiếng Việt**: Sử dụng thanh cấu trúc (Outline panel) để nhận diện nhanh các lớp (Class), hàm (Function) hay thuộc tính. Bạn có thể nhấn đúp vào ký hiệu để con trỏ soạn thảo nhảy trực tiếp tới dòng khoa báo tương ứng.
*   **English**: Use the dedicated Symbols Column to detect classes, variables, and methods inside your files. Double-click any symbol to instantly snap the editor's cursor to the corresponding line.

### 4. ⚡ Chạy Thử Web Tức Thì | HTML/CSS/JS Instant Preview
*   **Tiếng Việt**: Trình xem trước Web thời gian thực xử lý mượt mà toàn bộ liên kết nội bộ giữa các file HTML, CSS và JavaScript. Bạn có thể thay đổi giao diện và quan sát chuyển động UI phản hồi trực quan ngay lập tức.
*   **English**: Real-time sandboxed Web viewer resolves external linked stylesheets and script files seamlessly. Modify code on-the-fly and watch your layout render and respond instantly.

### 5. 🤖 Trợ Lý Trí Tuệ Nhân Tạo | Gemini AI Developer Expert
*   **Tiếng Việt**: Trò chuyện trực tiếp cùng trợ lý lập trình chuyên sâu của Google Gemini. Tối ưu thuật toán, phát hiện lỗ hổng bảo mật, giải thích mã phức tạp hay thậm chí phục dựng tính năng mới an toàn ngay trên thiết bị.
*   **English**: Chat, debug, and refactor directly with Google’s Gemini AI integrated model. Let the assistant analyze performance bottlenecks, translate structures, or write clean code easily.

### 6. 🐙 Đồng Bộ Hóa GitHub | Two-Way GitHub Repository Sync
*   **Tiếng Việt**: Kết nối trực tiếp tài khoản cá nhân thông qua GitHub Access Token. Bạn có thể nhân bản (Clone) kho chứa trực tuyến, theo dõi thay đổi trực quan (Diff panel), cập nhật dữ liệu (Git Pull) và lưu trữ trực tiếp (Git Push) mã nguồn phiên bản mới nhất.
*   **English**: Connect securely using your GitHub Access Token. Clone repositories, preview file changes side-by-side inside the beautiful Diff interface, pull updates, and push your commits directly to remote origins.

---

## 🛠️ HƯỚNG DẪN BẮT ĐẦU | QUICK START GUIDE

1.  📂 **Mở tệp `index.html`** trong Sidebar bên trái và bắt đầu tinh chỉnh nội dung.
2.  🎨 **Tùy biến `css/style.css`** để biến đổi phong cách thiết kế, phông nền hay màu chuyển sắc.
3.  ⚙️ **Truy cập Cài đặt (Settings)** ở góc dưới để kích hoạt Chế độ tiếng Việt / tiếng Anh, hoặc chuyển đổi giao diện sáng/tối.
4.  🔒 **Mã bảo mật PIN**: Thiết lập mã PIN khóa màn hình trong mục Bảo mật để mã hóa không gian làm việc cục bộ của bạn an toàn nhất.

*Chúc bạn có những trải nghiệm thăng hoa và sáng tạo cùng Code Studio!*
*Have a wonderful journey pushing the boundaries of coding with Code Studio!*

---
## 📜 BẢN QUYỀN & GIẤY PHÉP | LICENSE & CREDITS
*   **Tác giả (Author)**: Nguyễn Hồng Diễm Phúc 🇻🇳
*   **Giấy phép (License)**: MIT License (2026)
""",
                language = "markdown"
            ),
            FileEntity(
                projectId = projId,
                name = "index.html",
                path = "index.html",
                isFolder = false,
                content = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Code Studio - Premium Mobile IDE</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <!-- Language Selector Floating Header -->
    <header class="header-nav">
        <div class="logo-wrapper">
            <svg class="glowing-logo" viewBox="0 0 100 100" width="36" height="36">
                <defs>
                    <linearGradient id="logo-grad" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stop-color="#388bfd" />
                        <stop offset="100%" stop-color="#a371f7" />
                    </linearGradient>
                    <filter id="glow">
                        <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
                        <feMerge>
                            <feMergeNode in="coloredBlur"/>
                            <feMergeNode in="SourceGraphic"/>
                        </feMerge>
                    </filter>
                </defs>
                <rect x="15" y="15" width="70" height="70" rx="18" fill="url(#logo-grad)" filter="url(#glow)"/>
                <path d="M40 38 L30 50 L40 62 M60 38 L70 50 L60 62" stroke="white" stroke-width="6" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
                <rect x="46" y="56" width="10" height="4" fill="white" rx="1"/>
            </svg>
            <span class="app-brand-name">Code Studio</span>
        </div>
        <div class="lang-controls">
            <button class="lang-btn active" id="btn-lang-en">EN</button>
            <span class="lang-divider">|</span>
            <button class="lang-btn" id="btn-lang-vi">VN</button>
        </div>
    </header>

    <main class="container">
        <!-- Hero Section -->
        <section class="hero-section">
            <h1 class="hero-title" id="txt-hero-title">Elevate Coding Everywhere</h1>
            <p class="hero-subtitle" id="txt-hero-subtitle">The ultimate offline-first mobile IDE featuring Monaco editor power, GitHub integration, and specialized Gemini AI assistance.</p>
            <div class="hero-badge" id="txt-hero-badge">⚡ Crafted by Nguyen Hong Diem Phuc</div>
        </section>

        <!-- Dynamic Core Feature Preview Card -->
        <section class="preview-card">
            <div class="card-glow"></div>
            <div class="card-interior">
                <div class="terminal-bar">
                    <span class="dot red"></span>
                    <span class="dot yellow"></span>
                    <span class="dot green"></span>
                    <span class="window-title" id="txt-window-title">code_studio_preview.js</span>
                </div>
                
                <div class="card-body">
                    <h2 class="card-title" id="txt-card-title">Experience the Fluidity</h2>
                    <p class="card-desc" id="txt-card-desc">Click the buttons below to switch themes or test interactive messages in our live playground console.</p>
                    
                    <div class="playground-console">
                        <div class="console-overlay"></div>
                        <div class="console-header">
                            <span class="status-dot pulsing"></span>
                            <span class="console-label">PLAYGROUND INPUT / OUTPUT</span>
                        </div>
                        <div class="console-output" id="output-box">
                            System initialized. Welcome to Code Studio!
                        </div>
                    </div>

                    <div class="control-actions">
                        <button class="action-btn btn-primary" id="btn-theme-cycle">
                            <span class="icon">🎨</span>
                            <span id="txt-btn-theme">Cycle Theme</span>
                        </button>
                        <button class="action-btn btn-secondary" id="btn-msg-cycle">
                            <span class="icon">🤖</span>
                            <span id="txt-btn-msg">Next Quote</span>
                        </button>
                    </div>
                </div>
            </div>
        </section>

        <!-- Features Showcase Grid -->
        <section class="features-grid">
            <div class="feature-box">
                <div class="feature-icon-wrap">💻</div>
                <h3 id="feat-title-1">Premium Editor</h3>
                <p id="feat-desc-1">Powered by Monaco engine, custom font-sizing, and double-tap symbol structure navigation.</p>
            </div>
            <div class="feature-box">
                <div class="feature-icon-wrap">⚡</div>
                <h3 id="feat-title-2">Live Web Preview</h3>
                <p id="feat-desc-2">Run HTML, CSS, JavaScript instantaneously without external server overheads.</p>
            </div>
            <div class="feature-box">
                <div class="feature-icon-wrap">🤖</div>
                <h3 id="feat-title-3">Gemini Expert AI</h3>
                <p id="feat-desc-3">Refactor structures, explain logic, and debug errors safely inside your view.</p>
            </div>
            <div class="feature-box">
                <div class="feature-icon-wrap">🐙</div>
                <h3 id="feat-title-4">Git Repository Synchronization</h3>
                <p id="feat-desc-4">Clone, pull, diff, commit and push modifications directly to GitHub origins.</p>
            </div>
        </section>
    </main>

    <footer class="footer">
        <p class="footer-text">
            © 2026 Nguyễn Hồng Diễm Phúc. All rights reserved. Registered under MIT License.
        </p>
    </footer>

    <script src="js/script.js"></script>
</body>
</html>
""",
                language = "html"
            ),
            FileEntity(
                projectId = projId,
                name = "css",
                path = "css",
                isFolder = true,
                content = "",
                language = ""
            ),
            FileEntity(
                projectId = projId,
                name = "style.css",
                path = "css/style.css",
                isFolder = false,
                content = """:root {
    --bg-primary: #0b0f19;
    --card-bg: rgba(17, 24, 39, 0.7);
    --text-primary: #f3f4f6;
    --text-secondary: #9ca3af;
    --accent: #388bfd;
    --accent-glow: rgba(56, 139, 253, 0.4);
    --border-color: rgba(255, 255, 255, 0.08);
}

* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
    background-color: var(--bg-primary);
    background-image: 
        radial-gradient(circle at 10% 20%, rgba(56, 139, 253, 0.15) 0%, transparent 40%),
        radial-gradient(circle at 90% 80%, rgba(163, 113, 247, 0.15) 0%, transparent 40%);
    background-attachment: fixed;
    color: var(--text-primary);
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-bottom: 40px;
}

/* Header & Language controls */
.header-nav {
    width: 100%;
    max-width: 1100px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 24px;
    border-bottom: 1px solid var(--border-color);
    backdrop-filter: blur(8px);
    margin-bottom: 20px;
}

.logo-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
}

.glowing-logo {
    filter: drop-shadow(0px 0px 8px var(--accent-glow));
    transition: transform 0.5s ease;
}

.glowing-logo:hover {
    transform: rotate(15deg) scale(1.1);
}

.app-brand-name {
    font-size: 1.3rem;
    font-weight: 800;
    background: linear-gradient(135deg, #4f9cfd, #c084fc);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    letter-spacing: -0.5px;
}

.lang-controls {
    display: flex;
    align-items: center;
    gap: 8px;
    background: rgba(255, 255, 255, 0.04);
    padding: 4px 12px;
    border-radius: 99px;
    border: 1px solid var(--border-color);
}

.lang-btn {
    background: none;
    border: none;
    color: var(--text-secondary);
    font-weight: 700;
    font-size: 0.85rem;
    cursor: pointer;
    transition: color 0.3s, transform 0.2s;
    padding: 4px 8px;
}

.lang-btn.active {
    color: #4f9cfd;
    text-shadow: 0 0 8px rgba(79, 156, 253, 0.5);
}

.lang-btn:hover {
    color: var(--text-primary);
    transform: scale(1.05);
}

.lang-divider {
    color: rgba(255, 255, 255, 0.2);
    font-size: 0.8rem;
    user-select: none;
}

/* Container */
.container {
    width: 100%;
    max-width: 900px;
    padding: 0 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    flex: 1;
}

/* Hero Section */
.hero-section {
    text-align: center;
    margin: 30px 0 40px 0;
}

.hero-title {
    font-size: 2.8rem;
    font-weight: 900;
    letter-spacing: -1.5px;
    line-height: 1.15;
    background: linear-gradient(to right, #ffffff, #94a3b8);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    margin-bottom: 16px;
    animation: fadeIn 1s ease-out;
}

.hero-subtitle {
    font-size: 1.1rem;
    color: var(--text-secondary);
    line-height: 1.6;
    max-width: 650px;
    margin: 0 auto 20px auto;
    animation: fadeIn 1.2s ease-out;
}

.hero-badge {
    display: inline-block;
    background: rgba(163, 113, 247, 0.15);
    color: #c084fc;
    border: 1px solid rgba(163, 113, 247, 0.3);
    padding: 6px 14px;
    border-radius: 99px;
    font-size: 0.8rem;
    font-weight: 700;
    letter-spacing: 0.5px;
    text-transform: uppercase;
}

/* Card Section */
.preview-card {
    position: relative;
    width: 100%;
    border-radius: 20px;
    overflow: hidden;
    margin-bottom: 50px;
}

.card-glow {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, rgba(88, 166, 255, 0.1), rgba(163, 113, 247, 0.1));
    border-radius: 20px;
    z-index: 1;
    pointer-events: none;
}

.card-interior {
    background: var(--card-bg);
    border: 1px solid var(--border-color);
    border-radius: 20px;
    backdrop-filter: blur(20px);
    overflow: hidden;
    position: relative;
    z-index: 2;
}

.terminal-bar {
    background: rgba(15, 23, 42, 0.8);
    padding: 12px 18px;
    display: flex;
    align-items: center;
    border-bottom: 1px solid var(--border-color);
}

.dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    margin-right: 6px;
}

.dot.red { background-color: #ff5f56; }
.dot.yellow { background-color: #ffbd2e; }
.dot.green { background-color: #27c93f; }

.window-title {
    font-family: monospace;
    font-size: 0.8rem;
    color: var(--text-secondary);
    margin-left: 12px;
}

.card-body {
    padding: 30px;
}

.card-title {
    font-size: 1.5rem;
    font-weight: 800;
    margin-bottom: 8px;
    letter-spacing: -0.5px;
}

.card-desc {
    color: var(--text-secondary);
    font-size: 0.95rem;
    margin-bottom: 24px;
    line-height: 1.5;
}

/* Micro Console */
.playground-console {
    background: #060913;
    border-radius: 12px;
    border: 1px solid rgba(255, 255, 255, 0.05);
    padding: 18px;
    min-height: 110px;
    position: relative;
    margin-bottom: 24px;
    overflow: hidden;
    display: flex;
    flex-direction: column;
}

.console-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
}

.status-dot {
    width: 8px;
    height: 8px;
    background-color: #4f9cfd;
    border-radius: 50%;
}

.status-dot.pulsing {
    box-shadow: 0 0 0 0 rgba(79, 156, 253, 0.7);
    animation: pulse 1.6s infinite;
}

.console-label {
    font-family: monospace;
    font-size: 0.7rem;
    color: #4f9cfd;
    letter-spacing: 1px;
    font-weight: bold;
}

.console-output {
    font-family: 'Consolas', 'Fira Code', monospace;
    font-size: 0.9rem;
    color: #a5b4fc;
    line-height: 1.5;
    flex: 1;
    word-break: break-word;
    transition: color 0.3s ease;
}

.console-overlay {
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    background: radial-gradient(circle at center, transparent 60%, rgba(0,0,0,0.3) 100%);
    pointer-events: none;
}

/* Buttons */
.control-actions {
    display: flex;
    gap: 14px;
    justify-content: center;
}

.action-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 24px;
    font-size: 0.9rem;
    font-weight: 700;
    border-radius: 10px;
    border: none;
    cursor: pointer;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.action-btn .icon {
    font-size: 1.1rem;
}

.action-btn.btn-primary {
    background: linear-gradient(135deg, #388bfd, #5c6bc0);
    color: white;
    box-shadow: 0 4px 15px rgba(56, 139, 253, 0.25);
}

.action-btn.btn-primary:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(56, 139, 253, 0.4);
}

.action-btn.btn-secondary {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid var(--border-color);
    color: var(--text-primary);
}

.action-btn.btn-secondary:hover {
    background: rgba(255, 255, 255, 0.1);
    transform: translateY(-2px);
}

/* Feature Showcase Grid */
.features-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    width: 100%;
}

.feature-box {
    background: rgba(255,255,255,0.02);
    border: 1px solid var(--border-color);
    border-radius: 16px;
    padding: 24px;
    backdrop-filter: blur(10px);
    transition: all 0.3s ease;
}

.feature-box:hover {
    background: rgba(255,255,255,0.04);
    border-color: rgba(56, 139, 253, 0.2);
    transform: translateY(-3px);
}

.feature-icon-wrap {
    font-size: 1.8rem;
    margin-bottom: 12px;
}

.feature-box h3 {
    font-size: 1.1rem;
    font-weight: 700;
    margin-bottom: 8px;
    letter-spacing: -0.2px;
}

.feature-box p {
    font-size: 0.85rem;
    color: var(--text-secondary);
    line-height: 1.5;
}

/* Footer style */
.footer {
    width: 100%;
    text-align: center;
    padding: 40px 20px 0 20px;
    border-top: 1px solid var(--border-color);
    margin-top: 50px;
}

.footer-text {
    font-size: 0.8rem;
    color: #4b5563;
    line-height: 1.5;
    font-family: monospace;
}

/* Animations */
@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}

@keyframes pulse {
    0% {
        transform: scale(0.95);
        box-shadow: 0 0 0 0 rgba(79, 156, 253, 0.7);
    }
    70% {
        transform: scale(1);
        box-shadow: 0 0 0 6px rgba(79, 156, 253, 0);
    }
    100% {
        transform: scale(0.95);
        box-shadow: 0 0 0 0 rgba(79, 156, 253, 0);
    }
}

/* Responsive adjustment */
@media(max-width: 600px) {
    .features-grid {
        grid-template-columns: 1fr;
    }
    .hero-title {
        font-size: 2rem;
    }
    .hero-subtitle {
        font-size: 0.95rem;
    }
}
""",
                language = "css"
            ),
            FileEntity(
                projectId = projId,
                name = "js",
                path = "js",
                isFolder = true,
                content = "",
                language = ""
            ),
            FileEntity(
                projectId = projId,
                name = "script.js",
                path = "js/script.js",
                isFolder = false,
                content = """document.addEventListener("DOMContentLoaded", () => {
    // Buttons & Outputs
    const btnThemeCycle = document.getElementById("btn-theme-cycle");
    const btnMsgCycle = document.getElementById("btn-msg-cycle");
    const outputBox = document.getElementById("output-box");

    // Language Toggle Buttons
    const btnLangEn = document.getElementById("btn-lang-en");
    const btnLangVi = document.getElementById("btn-lang-vi");

    // Bilingual Elements & Translations
    const elementsToTranslate = {
        "txt-hero-title": {
            en: "Elevate Coding Everywhere",
            vi: "Nâng Tầm Lập Trình Mọi Nơi"
        },
        "txt-hero-subtitle": {
            en: "The ultimate offline-first mobile IDE featuring Monaco editor power, GitHub integration, and specialized Gemini AI assistance.",
            vi: "Môi trường lập trình di động offline-first tối tân tích hợp sức mạnh nhân Monaco, đồng bộ GitHub và Trợ lý Chuyên gia Gemini AI."
        },
        "txt-hero-badge": {
            en: "⚡ Crafted by Nguyen Hong Diem Phuc",
            vi: "⚡ Thiết kế bởi Nguyễn Hồng Diễm Phúc"
        },
        "txt-window-title": {
            en: "code_studio_preview.js",
            vi: "code_studio_ban_nhap.js"
        },
        "txt-card-title": {
            en: "Experience the Fluidity",
            vi: "Trải Nghiệm Sự Mượt Mà"
        },
        "txt-card-desc": {
            en: "Click the buttons below to switch themes or test interactive messages in our live playground console.",
            vi: "Bấm nút bên dưới để chuyển đổi màu nền hoặc chạy thử thông điệp trong bảng trải nghiệm trực quan."
        },
        "txt-btn-theme": {
            en: "Cycle Theme",
            vi: "Thay Màu Nền"
        },
        "txt-btn-msg": {
            en: "Next Quote",
            vi: "Thông Điệp Mới"
        },
        "feat-title-1": {
            en: "Premium Editor",
            vi: "Soạn Thảo Đẳng Cấp"
        },
        "feat-desc-1": {
            en: "Powered by Monaco engine, custom font-sizing, and double-tap symbol structure navigation.",
            vi: "Hỗ trợ nhân Monaco cao cấp, đổi cỡ chữ linh hoạt, và chạm đúp định vị cấu hình tệp nhanh."
        },
        "feat-title-2": {
            en: "Live Web Preview",
            vi: "Trình Xem Trước Sống Động"
        },
        "feat-desc-2": {
            en: "Run HTML, CSS, JavaScript instantaneously without external server overheads.",
            vi: "Chạy thử các tệp HTML, CSS, JavaScript lập tức mà không cần máy chủ trung gian."
        },
        "feat-title-3": {
            en: "Gemini Expert AI",
            vi: "Chuyên Gia Gemini AI"
        },
        "feat-desc-3": {
            en: "Refactor structures, explain logic, and debug errors safely inside your view.",
            vi: "Tối ưu hóa mã nguồn, giải thích cách thức hoạt động và dò lỗi thông minh trực quan."
        },
        "feat-title-4": {
            en: "Git Synchronization",
            vi: "Đồng Bộ Hóa Git"
        },
        "feat-desc-4": {
            en: "Clone, pull, diff, commit and push modifications directly to GitHub origins.",
            vi: "Sao chép, theo dõi nhánh thay đổi, lưu trữ và đẩy mã trực tiếp lên kho lưu trữ GitHub."
        }
    };

    // Messages
    const quotes = {
        en: [
            "Ready to write beautiful code on your mobile device!",
            "Double click or tap Outline Symbols to navigate your declarations instantly.",
            "Set your private PIN access token inside Security to restrict folder access.",
            "Your credentials keys are absolute safe and localized offline on Room DB.",
            "We make programming accessible anytime, anywhere! 🚀"
        ],
        vi: [
            "Sẵn sàng viết code cực đẹp ngay trên thiết bị di động của bạn!",
            "Nhấp đúp hoặc chạm vào Cây Kí Hiệu để đi tới dòng khai báo ngay lập tức.",
            "Cài đặt khóa PIN bảo mật an toàn chống mọi sự xâm nhập phi pháp.",
            "Khóa API Gemini và Token GitHub được lưu cục bộ an toàn tuyệt đối.",
            "Đưa lập trình tiếp cận mọi người, mọi lúc, mọi nơi! 🚀"
        ]
    };

    const gradientThemes = [
        {
            bg: "radial-gradient(circle at 10% 20%, rgba(56, 139, 253, 0.15) 0%, transparent 40%), radial-gradient(circle at 90% 80%, rgba(163, 113, 247, 0.15) 0%, transparent 40%)",
            color: "#0b0f19",
            msg: { en: "Switched to Deep Cosmos theme.", vi: "Đã chuyển sang chủ đề Vũ Trụ Sâu." }
        },
        {
            bg: "radial-gradient(circle at 10% 20%, rgba(220, 38, 38, 0.15) 0%, transparent 45%), radial-gradient(circle at 90% 70%, rgba(245, 158, 11, 0.1) 0%, transparent 40%)",
            color: "#110707",
            msg: { en: "Switched to Crimson Nebula theme.", vi: "Đã chuyển sang chủ đề Tinh Vân Đỏ." }
        },
        {
            bg: "radial-gradient(circle at 10% 20%, rgba(16, 185, 129, 0.15) 0%, transparent 45%), radial-gradient(circle at 80% 80%, rgba(6, 182, 212, 0.15) 0%, transparent 45%)",
            color: "#040d0f",
            msg: { en: "Switched to Emerald Tech theme.", vi: "Đã chuyển sang chủ đề Công Nghệ Lục Bảo." }
        },
        {
            bg: "radial-gradient(circle at 15% 15%, rgba(236, 72, 153, 0.15) 0%, transparent 50%), radial-gradient(circle at 85% 85%, rgba(99, 102, 241, 0.15) 0%, transparent 50%)",
            color: "#0f0c1b",
            msg: { en: "Switched to Sunset Violet theme.", vi: "Đã chuyển sang chủ đề Hoàng Hôn Tím." }
        }
    ];

    let currentLang = "en";
    let themeIndex = 0;
    let quoteIndex = 0;

    function setLanguage(lang) {
        currentLang = lang;
        if (lang === "en") {
            btnLangEn.classList.add("active");
            btnLangVi.classList.remove("active");
            document.documentElement.lang = "en";
        } else {
            btnLangEn.classList.remove("active");
            btnLangVi.classList.add("active");
            document.documentElement.lang = "vi";
        }

        for (const [id, value] of Object.entries(elementsToTranslate)) {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = value[lang];
            }
        }
        
        outputBox.textContent = currentLang === "en" ? "Language switched to English!" : "Đã chuyển ngôn ngữ sang Tiếng Việt!";
    }

    btnLangEn.addEventListener("click", () => setLanguage("en"));
    btnLangVi.addEventListener("click", () => setLanguage("vi"));

    btnThemeCycle.addEventListener("click", () => {
        themeIndex = (themeIndex + 1) % gradientThemes.length;
        const selected = gradientThemes[themeIndex];
        
        document.body.style.backgroundColor = selected.color;
        document.body.style.backgroundImage = selected.bg;
        
        outputBox.textContent = selected.msg[currentLang];
        outputBox.style.color = "#a5b4fc";
    });

    btnMsgCycle.addEventListener("click", () => {
        const langQuotes = quotes[currentLang];
        quoteIndex = (quoteIndex + 1) % langQuotes.length;
        
        outputBox.textContent = langQuotes[quoteIndex];
        outputBox.style.color = "#388bfd";
    });

    setLanguage("vi");
});
"""
            )
        )

        for (file in filesToSeed) {
            repository.insertFile(file)
        }

        val projectsList = repository.allProjects.first()
        projectsList.firstOrNull()?.let { selectProject(it) }
    }

    fun selectProject(project: ProjectEntity) {
        currentProject = project
        githubUsername = project.githubUsername
        githubToken = project.githubToken

        openFiles = emptyList()
        projectFilesJob?.cancel()
        projectFilesJob = viewModelScope.launch {
            // Fetch first emission of files one-time to set active file correctly
            val initialFiles = repository.getFilesForProject(project.id).first()
            if (activeFile == null || activeFile?.projectId != project.id) {
                val readme = initialFiles.find { it.name == "README.md" }
                val firstFile = readme ?: initialFiles.find { !it.isFolder }
                setFileActive(firstFile)
            }

            // Collect to flow to update file tree, without triggering file switching on updates
            repository.getFilesForProject(project.id).collect { files ->
                _projectFiles.value = files
            }
        }
    }

    fun createProject(name: String, desc: String, githubUsername: String = "", githubToken: String = "") {
        viewModelScope.launch {
            val projId = repository.insertProject(ProjectEntity(
                name = name, 
                description = desc,
                githubUsername = githubUsername,
                githubToken = githubToken
            ))
            val mainReadme = FileEntity(
                projectId = projId,
                name = "README.md",
                path = "README.md",
                isFolder = false,
                content = "# $name\n\nDự án tạo mới tại Monaco IDE.",
                language = "markdown"
            )
            repository.insertFile(mainReadme)
            val updatedList = repository.allProjects.first()
            val created = updatedList.find { it.id == projId }
            if (created != null) {
                selectProject(created)
            }
        }
    }

    fun deleteCurrentProject() {
        val proj = currentProject ?: return
        viewModelScope.launch {
            repository.deleteProject(proj.id)
            val updatedList = repository.allProjects.first()
            if (updatedList.isNotEmpty()) {
                selectProject(updatedList.first())
            } else {
                seedDefaultWorkspace()
            }
        }
    }

    // Caret placement signals
    var requestGoToLine by mutableStateOf<Int?>(null)

    // Original file contents mapping for Git Diff tracking
    val originalFileContent = mutableStateMapOf<Long, String>()

    fun getOriginalContent(fileId: Long): String {
        return originalFileContent[fileId] ?: ""
    }

    var openFiles by mutableStateOf<List<FileEntity>>(emptyList())

    fun setFileActive(file: FileEntity?) {
        // Save old file first
        saveActiveFileChanges()

        activeFile = file
        if (file != null) {
            editorText = file.content
            editorLanguage = detectLanguage(file.name)
            
            // Snapshot original content when first loaded
            if (!originalFileContent.containsKey(file.id)) {
                originalFileContent[file.id] = file.content
            }

            // Manage active files inside tabs
            if (!openFiles.any { it.id == file.id }) {
                openFiles = openFiles + file
            }
        } else {
            editorText = ""
        }
    }

    fun closeFile(file: FileEntity) {
        val wasActive = activeFile?.id == file.id
        openFiles = openFiles.filter { it.id != file.id }
        if (wasActive) {
            val nextActive = openFiles.lastOrNull { !it.isFolder }
            setFileActive(nextActive)
        }
    }

    fun renameFile(file: FileEntity, newName: String) {
        if (newName.isEmpty()) return
        viewModelScope.launch {
            val oldPath = file.path
            val dir = if (oldPath.contains("/")) oldPath.substringBeforeLast("/") else ""
            val newPath = if (dir.isEmpty()) newName else "$dir/$newName"
            val updated = file.copy(
                name = newName,
                path = newPath,
                language = detectLanguage(newName)
            )
            repository.updateFile(updated)

            // Update in open tabs list
            openFiles = openFiles.map { if (it.id == file.id) updated else it }
            if (activeFile?.id == file.id) {
                activeFile = updated
                editorLanguage = detectLanguage(newName)
            }
        }
    }

    fun deleteFileEntity(file: FileEntity) {
        viewModelScope.launch {
            repository.deleteFile(file.id)
            if (file.isFolder) {
                val prefix = file.path + "/"
                val children = _projectFiles.value.filter { it.path.startsWith(prefix) }
                for (child in children) {
                    repository.deleteFile(child.id)
                }
            }

            // Remove from tabs
            val openFilesToKeep = openFiles.filter { 
                it.id != file.id && (!file.isFolder || !it.path.startsWith(file.path + "/")) 
            }
            openFiles = openFilesToKeep

            if (activeFile?.id == file.id || (file.isFolder && activeFile?.path?.startsWith(file.path + "/") == true)) {
                val nextActive = openFilesToKeep.lastOrNull { !it.isFolder }
                setFileActive(nextActive)
            }
        }
    }

    fun addNewFileOrFolder(name: String, parentFolderPath: String, isFolder: Boolean) {
        val proj = currentProject ?: return
        viewModelScope.launch {
            val finalPath = if (parentFolderPath.isEmpty()) name else "$parentFolderPath/$name"
            if (_projectFiles.value.any { it.path == finalPath && it.isFolder == isFolder }) {
                return@launch
            }

            val newFile = FileEntity(
                projectId = proj.id,
                name = name,
                path = finalPath,
                isFolder = isFolder,
                content = "",
                language = if (isFolder) "folder" else detectLanguage(name)
            )
            val id = repository.insertFile(newFile)
            val fileWithId = newFile.copy(id = id)
            if (!isFolder) {
                setFileActive(fileWithId)
            }
        }
    }

    fun addNewFileWithContent(name: String, path: String, fileContent: String) {
        val proj = currentProject ?: return
        viewModelScope.launch {
            val newFile = FileEntity(
                projectId = proj.id,
                name = name,
                path = path,
                isFolder = false,
                content = fileContent,
                language = detectLanguage(name)
            )
            val id = repository.insertFile(newFile)
            val fileWithId = newFile.copy(id = id)
            setFileActive(fileWithId)
        }
    }

    fun saveActiveFileChanges() {
        val file = activeFile ?: return
        if (file.content != editorText) {
            val updated = file.copy(content = editorText, lastModified = System.currentTimeMillis())
            activeFile = updated
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateFile(updated)
            }
        }
    }

    // Undo/Redo tracking
    private val undoStack = java.util.Stack<String>()
    private val redoStack = java.util.Stack<String>()
    private var isUndoRedoActive = false
    private var lastSavedText = ""

    fun handleTextEdit(newText: String) {
        if (!isUndoRedoActive && newText != editorText) {
            val shouldPush = undoStack.isEmpty() || 
                    kotlin.math.abs(newText.length - lastSavedText.length) > 10 ||
                    (newText.endsWith(" ") && !editorText.endsWith(" ")) ||
                    (newText.endsWith("\n") && !editorText.endsWith("\n"))
            if (shouldPush) {
                if (undoStack.size > 200) {
                    undoStack.removeAt(0)
                }
                undoStack.push(editorText)
                lastSavedText = newText
                redoStack.clear()
            }
        }
        editorText = newText
        val file = activeFile ?: return
        val updated = file.copy(content = newText, lastModified = System.currentTimeMillis())
        activeFile = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFile(updated)
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            isUndoRedoActive = true
            val prev = undoStack.pop()
            redoStack.push(editorText)
            editorText = prev
            lastSavedText = prev
            val file = activeFile
            if (file != null) {
                val updated = file.copy(content = prev, lastModified = System.currentTimeMillis())
                activeFile = updated
                viewModelScope.launch(Dispatchers.IO) {
                    repository.updateFile(updated)
                }
            }
            isUndoRedoActive = false
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            isUndoRedoActive = true
            val nextText = redoStack.pop()
            undoStack.push(editorText)
            editorText = nextText
            lastSavedText = nextText
            val file = activeFile
            if (file != null) {
                val updated = file.copy(content = nextText, lastModified = System.currentTimeMillis())
                activeFile = updated
                viewModelScope.launch(Dispatchers.IO) {
                    repository.updateFile(updated)
                }
            }
            isUndoRedoActive = false
        }
    }

    fun changeEditorLanguage(lang: String) {
        editorLanguage = lang
        val file = activeFile ?: return
        val updated = file.copy(language = lang)
        activeFile = updated
        viewModelScope.launch {
            repository.updateFile(updated)
        }
    }

    // Add new files to the active project workspace
    fun addNewFile(name: String, path: String) {
        val proj = currentProject ?: return
        viewModelScope.launch {
            val newFile = FileEntity(
                projectId = proj.id,
                name = name,
                path = path,
                isFolder = false,
                content = "",
                language = detectLanguage(name)
            )
            val id = repository.insertFile(newFile)
            val fileWithId = newFile.copy(id = id)
            setFileActive(fileWithId)
        }
    }

    fun deleteCurrentFile() {
        val file = activeFile ?: return
        viewModelScope.launch {
            repository.deleteFile(file.id)
            activeFile = null
            editorText = ""
            val files = _projectFiles.value
            val firstRemaining = files.find { it.id != file.id && !it.isFolder }
            setFileActive(firstRemaining)
        }
    }

    // Settings persistence
    fun saveGithubToken(token: String) {
        githubToken = token
        currentProject?.let { proj ->
            currentProject = proj.copy(githubToken = token)
            viewModelScope.launch {
                repository.updateProject(currentProject!!)
            }
        }
    }

    fun saveGithubUsername(username: String) {
        githubUsername = username
        currentProject?.let { proj ->
            currentProject = proj.copy(githubUsername = username)
            viewModelScope.launch {
                repository.updateProject(currentProject!!)
            }
        }
    }

    fun saveGeminiApiKey(key: String) {
        geminiApiKey = key
        val prefs = getApplication<Application>().getSharedPreferences("monaco_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("gemini_api_key", key).apply()
    }

    fun saveEditorTheme(theme: String) {
        editorTheme = theme
        val prefs = getApplication<Application>().getSharedPreferences("monaco_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("editor_theme", theme).apply()
    }

    fun saveFontSize(size: Int) {
        fontSize = size
        val prefs = getApplication<Application>().getSharedPreferences("monaco_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("editor_font_size", size).apply()
    }

    fun saveAutoHighlight(enabled: Boolean) {
        runAutoHighlight = enabled
        val prefs = getApplication<Application>().getSharedPreferences("monaco_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_highlight", enabled).apply()
    }

    fun toggleLanguage(isViet: Boolean) {
        isVietnamese = isViet
        val prefs = getApplication<Application>().getSharedPreferences("monaco_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_vietnamese", isViet).apply()
        if (_geminiChat.value.size == 1 && (_geminiChat.value.first().sender == "gemini" || _geminiChat.value.first().sender == "system")) {
            _geminiChat.value = listOf(
                ChatMessage("system", if (isViet) "Chào bạn! Tôi là trợ lý AI chuyên gia của Code Studio, được phát triển bởi tác giả Phúc Nguyễn. Tôi có thể hỗ trợ giải thích code, viết code, sửa lỗi, và gợi ý tối ưu thuật toán. Hãy hỏi bất cứ điều gì!" else "Hello! I am Code Studio's expert AI assistant, developed by author Phuc Nguyen. I can help explain, write, debug code, and optimize algorithms. Ask me anything!")
            )
        }
    }

    fun completeFirstLaunch(name: String, githubUser: String, token: String) {
        val prefs = getApplication<Application>().getSharedPreferences("monaco_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        isFirstLaunch = false
        editor.putBoolean("is_first_launch", false).apply()

        // Create the first Workspace (Profile)
        viewModelScope.launch {
            val projId = repository.insertProject(
                ProjectEntity(
                    name = name, 
                    description = "Personal Workspace",
                    githubUsername = githubUser,
                    githubToken = token
                )
            )
            repository.insertFile(
                FileEntity(
                    projectId = projId,
                    name = "README.md",
                    path = "README.md",
                    isFolder = false,
                    content = "# Welcome to $name's Workspace\nBuilt with Code Studio 2.0",
                    language = "markdown"
                )
            )
            // Load this new workspace
            val proj = repository.allProjects.first().find { it.id == projId }
            if (proj != null) {
                selectProject(proj)
            }
        }
    }

    fun saveAppLock(hasLock: Boolean, pin: String) {
        hasAppLock = hasLock
        appLockPin = pin
        val prefs = getApplication<Application>().getSharedPreferences("monaco_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("has_app_lock", hasLock)
            .putString("app_lock_pin", pin)
            .apply()
    }

    fun unlockApp() {
        isAppUnlocked = true
    }

    // GitHub API Integration - Search Repositories
    fun fetchTrendingRepos(language: String = "") {
        isLoadingTrending = true
        viewModelScope.launch {
            try {
                // Randomize trending by fetching >50 stars updated recently and shuffling
                val calendar = java.util.Calendar.getInstance()
                val randomDays = (1..30).random()
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -randomDays)
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val dateString = format.format(calendar.time)
                
                var query = "pushed:>$dateString stars:>50"
                if (language.isNotEmpty()) {
                    query += " language:$language"
                }

                val authHeader = if (githubToken.isNotEmpty()) "Bearer $githubToken" else null
                val response = withContext(Dispatchers.IO) {
                    GithubRetrofitClient.service.searchRepositories(
                        query = query,
                        sort = "updated",
                        order = "desc",
                        token = authHeader
                    )
                }
                _trendingRepos.value = response.items.shuffled()
            } catch (e: Exception) {
                _trendingRepos.value = emptyList()
            } finally {
                isLoadingTrending = false
            }
        }
    }

    fun searchGithub() {
        if (githubQuery.isEmpty()) return

        viewModelScope.launch {
            try {
                val authHeader = if (githubToken.isNotEmpty()) "token $githubToken" else null
                val response = withContext(Dispatchers.IO) {
                    GithubRetrofitClient.service.searchRepositories(githubQuery, authHeader)
                }
                _githubRepos.value = response.items
            } catch (e: Exception) {
                githubQueryError = if (isVietnamese) "Không thể tìm kiếm Repo: ${e.message}" else "Unable to search repository: ${e.message}"
            } finally {
                isSearchingGithub = false
            }
        }
    }

    // GitHub Explore Repository contents
    fun exploreGithubRepo(repo: GithubRepoItem) {
        exploredRepo = repo
        exploredRepoPath = ""
        isShowingRepoCommits = false
        _githubCommits.value = emptyList()
        _githubPullRequests.value = emptyList()
        currentRepoDetail = null
        fetchExploredRepoContents()
        fetchGithubCommits()
        fetchGithubPullRequests()
        fetchRepoDetail()
    }

    private fun fetchGithubPullRequests() {
        val repo = exploredRepo ?: return
        isLoadingPullRequests = true
        viewModelScope.launch {
            try {
                val authHeader = if (githubToken.isNotEmpty()) "Bearer $githubToken" else null
                val prs = withContext(Dispatchers.IO) {
                    GithubRetrofitClient.service.getPullRequests(
                        owner = repo.owner.login,
                        repo = repo.name,
                        state = "all",
                        perPage = 30,
                        token = authHeader
                    )
                }
                _githubPullRequests.value = prs
            } catch (e: Exception) {
                _githubPullRequests.value = emptyList()
            } finally {
                isLoadingPullRequests = false
            }
        }
    }

    private fun fetchRepoDetail() {
        val repo = exploredRepo ?: return
        isLoadingRepoDetail = true
        viewModelScope.launch {
            try {
                val authHeader = if (githubToken.isNotEmpty()) "Bearer $githubToken" else null
                val detail = withContext(Dispatchers.IO) {
                    GithubRetrofitClient.service.getRepoDetail(
                        owner = repo.owner.login,
                        repo = repo.name,
                        token = authHeader
                    )
                }
                currentRepoDetail = detail
            } catch (e: Exception) {
                currentRepoDetail = null
            } finally {
                isLoadingRepoDetail = false
            }
        }
    }

    var isStarringRepo by mutableStateOf(false)
    fun starExploredRepo() {
        val repo = exploredRepo ?: return
        if (githubToken.isEmpty()) return
        isStarringRepo = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    GithubRetrofitClient.service.starRepo(
                        owner = repo.owner.login,
                        repo = repo.name,
                        token = "Bearer $githubToken"
                    )
                }
                // Refresh repo details to get updated stars count
                fetchRepoDetail()
            } catch (e: Exception) {
                // handle error
            } finally {
                isStarringRepo = false
            }
        }
    }

    var isForkingRepo by mutableStateOf(false)
    fun forkExploredRepo() {
        val repo = exploredRepo ?: return
        if (githubToken.isEmpty()) return
        isForkingRepo = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    GithubRetrofitClient.service.forkRepo(
                        owner = repo.owner.login,
                        repo = repo.name,
                        token = "Bearer $githubToken"
                    )
                }
                // We successfully forked it. We could switch to the user's new fork or just refresh
                fetchRepoDetail()
            } catch (e: Exception) {
                // handle error
            } finally {
                isForkingRepo = false
            }
        }
    }

    var isCreatingRepo by mutableStateOf(false)
    fun createGithubRepo(name: String, description: String, isPrivate: Boolean) {
        if (githubToken.isEmpty() || name.isEmpty()) return
        isCreatingRepo = true
        viewModelScope.launch {
            try {
                val newRepo = withContext(Dispatchers.IO) {
                    GithubRetrofitClient.service.createRepo(
                        request = com.example.api.GithubCreateRepoRequest(name, description, isPrivate, autoInit = true),
                        token = "Bearer $githubToken"
                    )
                }
                // Explore the newly created repo
                exploreGithubRepo(newRepo)
            } catch (e: Exception) {
                // handle error
            } finally {
                isCreatingRepo = false
            }
        }
    }

    private val _githubTreeCache = MutableStateFlow<Map<String, List<GithubContentItem>>>(emptyMap())
    val githubTreeCache: StateFlow<Map<String, List<GithubContentItem>>> = _githubTreeCache

    fun fetchGithubFolderItems(path: String) {
        val repo = exploredRepo ?: return
        if (_githubTreeCache.value.containsKey(path)) return
        viewModelScope.launch {
            try {
                val authHeader = if (githubToken.isNotEmpty()) "Bearer $githubToken" else null
                val contents = withContext(Dispatchers.IO) {
                    GithubRetrofitClient.service.getContents(
                        owner = repo.owner.login,
                        repo = repo.name,
                        path = path,
                        token = authHeader
                    )
                }
                val map = _githubTreeCache.value.toMutableMap()
                map[path] = contents
                _githubTreeCache.value = map
            } catch (e: Exception) {
            }
        }
    }

    fun goBackExploredRepo() {
        exploredRepo = null
        isShowingRepoCommits = false
        _exploredItems.value = emptyList()
        _githubCommits.value = emptyList()
        _githubTreeCache.value = emptyMap()
    }

    fun fetchExploredRepoContents() {
        val repo = exploredRepo ?: return
        isLoadingExploredItems = true
        viewModelScope.launch {
            try {
                val authHeader = if (githubToken.isNotEmpty()) "token $githubToken" else null
                val contents = withContext(Dispatchers.IO) {
                    GithubRetrofitClient.service.getContents(
                        owner = repo.owner.login,
                        repo = repo.name,
                        path = exploredRepoPath,
                        token = authHeader
                    )
                }
                _exploredItems.value = contents
            } catch (e: Exception) {
                _exploredItems.value = emptyList()
            } finally {
                isLoadingExploredItems = false
            }
        }
    }

    fun fetchGithubCommits() {
        val repo = exploredRepo ?: return
        isLoadingCommits = true
        viewModelScope.launch {
            try {
                val authHeader = if (githubToken.isNotEmpty()) "Bearer $githubToken" else null
                val commits = withContext(Dispatchers.IO) {
                    GithubRetrofitClient.service.getCommits(
                        owner = repo.owner.login,
                        repo = repo.name,
                        perPage = 15,
                        token = authHeader
                    )
                }
                _githubCommits.value = commits
            } catch (e: Exception) {
                _githubCommits.value = emptyList()
            } finally {
                isLoadingCommits = false
            }
        }
    }

    fun advanceExploredRepoPath(folderName: String) {
        exploredRepoPath = if (exploredRepoPath.isEmpty()) folderName else "$exploredRepoPath/$folderName"
        fetchExploredRepoContents()
    }

    // Download/Import GitHub file into current project workspace
    fun importGithubFile(item: GithubContentItem) {
        if (item.downloadUrl == null) return
        val proj = currentProject ?: return

        viewModelScope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    val client = okhttp3.OkHttpClient()
                    val req = okhttp3.Request.Builder().url(item.downloadUrl).build()
                    client.newCall(req).execute().body?.string() ?: ""
                }

                val newFile = FileEntity(
                    projectId = proj.id,
                    name = item.name,
                    path = item.path,
                    isFolder = false,
                    content = content,
                    language = detectLanguage(item.name)
                )
                val id = repository.insertFile(newFile)
                val fileWithId = newFile.copy(id = id)
                setFileActive(fileWithId)
                activeTab = SidebarTab.FILES
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    var isPushingToGithub by mutableStateOf(false)
    var githubPushSuccess by mutableStateOf<String?>(null)
    var githubPushError by mutableStateOf<String?>(null)

    
    fun commitAndPushAllWorkspaceFiles(
        owner: String,
        repo: String,
        commitMessage: String,
        branch: String = "main",
        onDone: (Boolean) -> Unit = {}
    ) {
        if (githubToken.isEmpty()) {
            githubPushError = "API Token không tồn tại."
            onDone(false)
            return
        }

        isPushingToGithub = true
        githubPushSuccess = null
        githubPushError = null

        viewModelScope.launch {
            try {
                val authHeader = "token $githubToken"
                val actualBranch = if (branch.trim().isEmpty()) "main" else branch.trim()
                
                // 1. Get branch ref -> base commit SHA
                val refResponse = withContext(Dispatchers.IO) {
                    com.example.api.GithubRetrofitClient.service.getBranchRef(owner, repo, actualBranch, authHeader)
                }
                val baseCommitSha = refResponse.obj.sha ?: ""

                // 2. Get base commit -> base tree SHA
                val commitResponse = withContext(Dispatchers.IO) {
                    com.example.api.GithubRetrofitClient.service.getCommit(owner, repo, baseCommitSha, authHeader)
                }
                val baseTreeSha = commitResponse.tree.sha ?: ""

                // 3. Prepare the tree of all local file contents
                val workspaceFiles = projectFiles.value.filter { !it.isFolder }
                val treeItems = workspaceFiles.map { file ->
                    com.example.api.GithubTreeItemRequest(
                        path = file.path,
                        mode = "100644",
                        type = "blob",
                        content = file.content
                    )
                }

                // 4. Create new tree based on the base tree
                val treeReq = com.example.api.GithubCreateTreeRequest(
                    baseTree = baseTreeSha,
                    tree = treeItems
                )
                val newTreeResponse = withContext(Dispatchers.IO) {
                    com.example.api.GithubRetrofitClient.service.createTree(owner, repo, treeReq, authHeader)
                }
                val newTreeSha = newTreeResponse.sha ?: ""

                // 5. Create new commit
                val commitReq = com.example.api.GithubCreateCommitRequest(
                    message = commitMessage,
                    tree = newTreeSha,
                    parents = listOf(baseCommitSha)
                )
                val newCommitResponse = withContext(Dispatchers.IO) {
                    com.example.api.GithubRetrofitClient.service.createCommit(owner, repo, commitReq, authHeader)
                }
                val newCommitSha = newCommitResponse.sha ?: ""

                // 6. Update reference
                val updateRefReq = com.example.api.GithubUpdateRefRequest(
                    sha = newCommitSha,
                    force = false
                )
                withContext(Dispatchers.IO) {
                    com.example.api.GithubRetrofitClient.service.updateRef(owner, repo, actualBranch, updateRefReq, authHeader)
                }

                githubPushSuccess = if (isVietnamese) "Đã commit và push tất cả thay đổi trên không gian làm việc lên nhánh '$actualBranch' thành công!" else "Successfully committed and pushed all workspace changes to segment branch '$actualBranch'!"
                fetchGithubCommits()
                onDone(true)
            } catch (e: Exception) {
                e.printStackTrace()
                githubPushError = if (isVietnamese) "Lỗi Auto-Commit toàn bộ: ${e.message}" else "Entire Auto-Commit error: ${e.message}"
                onDone(false)
            } finally {
                isPushingToGithub = false
            }
        }
    }


    // Gemini AI Integration
    fun sendGeminiChatMessage() {
        val promptText = geminiPrompt.trim()
        if (promptText.isEmpty()) return

        val userMessage = ChatMessage("user", promptText)
        _geminiChat.value = _geminiChat.value + userMessage
        geminiPrompt = ""
        isGeminiGenerating = true

        viewModelScope.launch {
            val systemPromptStr = if (isVietnamese) "Bạn là trợ lý AI chuyên gia Code của ứng dụng Code Studio được phát triển bởi tác giả Phúc Nguyễn. Chỉ tập trung trả lời ngắn gọn, thẳng vào vấn đề lập trình, tránh dài dòng. Code tạo ra cần chính xác và dễ copy." else "You are Code Studio's expert AI coding assistant, developed by Phuc Nguyen. Deliver brief, direct answers without fluff. Generated code must be highly accurate and easy to copy."
            
            val response = if (aiProvider == "copilot") {
                com.example.api.CopilotClient.generateContent(promptText, systemPromptStr)
            } else {
                GeminiRetrofitClient.generateContent(
                    prompt = promptText,
                    systemPrompt = systemPromptStr,
                    model = activeAiModel,
                    customApiKey = geminiApiKey,
                    isViet = isVietnamese
                )
            }
            _geminiChat.value = _geminiChat.value + ChatMessage(aiProvider, response)
            isGeminiGenerating = false
        }
    }

    fun askGeminiToExplainCode() {
        val currentCode = editorText
        if (currentCode.trim().isEmpty()) {
            _geminiChat.value = _geminiChat.value + ChatMessage(aiProvider, if (isVietnamese) "Editor trống. Hãy viết thêm code trước khi yêu cầu trợ giúp!" else "Editor is empty. Write some code before requesting support!")
            activeTab = SidebarTab.GEMINI
            return
        }

        activeTab = SidebarTab.GEMINI
        isGeminiGenerating = true
        val prompt = if (isVietnamese) "Giải thích chi tiết đoạn code sau, phân tích lỗi tiềm ẩn và định hướng tối ưu:\n\n```$editorLanguage\n$currentCode\n```" else "Explain this code in detail, analyze potential bugs, and provide optimization hints:\n\n```$editorLanguage\n$currentCode\n```"

        viewModelScope.launch {
            val systemPromptStr = if (isVietnamese) "Bạn là Trợ lý AI Code Studio. Phân tích lỗi và đề xuất sửa đổi code. Trả lời trực tiếp, rất ngắn gọn và tập trung chỉ vào chuyên môn." else "You are Code Studio AI Assistant. Analyze bugs and suggest edits. Answer directly and keep it highly concise."
            val response = if (aiProvider == "copilot") {
                com.example.api.CopilotClient.generateContent(prompt, systemPromptStr)
            } else {
                GeminiRetrofitClient.generateContent(
                    prompt = prompt,
                    systemPrompt = systemPromptStr,
                    model = activeAiModel,
                    customApiKey = geminiApiKey,
                    isViet = isVietnamese
                )
            }
            _geminiChat.value = _geminiChat.value + ChatMessage(aiProvider, response)
            isGeminiGenerating = false
        }
    }

    fun askGeminiToRefactorCode() {
        val currentCode = editorText
        if (currentCode.trim().isEmpty()) {
            _geminiChat.value = _geminiChat.value + ChatMessage(aiProvider, if (isVietnamese) "Editor trống. Hãy nhập code trước khi yêu cầu Refactor!" else "Editor is empty. Enter some code before refactoring!")
            activeTab = SidebarTab.GEMINI
            return
        }

        activeTab = SidebarTab.GEMINI
        isGeminiGenerating = true
        val prompt = if (isVietnamese) "Refactor nâng cấp mã nguồn sau để nâng cao hiệu suất, làm sạch cấu trúc, tuân thủ Clean Code. Cung cấp phiên bản code tối ưu nhất bên trong phần giải thích:\n\n```$editorLanguage\n$currentCode\n```" else "Refactor this code to boost performance, clean up structure, and adhere to Clean Code patterns. Keep the optimized code inside the explanation block:\n\n```$editorLanguage\n$currentCode\n```"

        viewModelScope.launch {
            val systemPromptStr = if (isVietnamese) "Trợ lý AI Code Studio. Đưa ra code tối ưu và sạch. Hạn chế tối đa các lời giải thích không cần thiết, đi thẳng vào vấn đề." else "Code Studio AI Assistant. Deliver clean, highly optimized code. Minimize explanation, go straight to the code solution."
            val response = if (aiProvider == "copilot") {
                com.example.api.CopilotClient.generateContent(prompt, systemPromptStr)
            } else {
                GeminiRetrofitClient.generateContent(
                    prompt = prompt,
                    systemPrompt = systemPromptStr,
                    model = activeAiModel,
                    customApiKey = geminiApiKey,
                    isViet = isVietnamese
                )
            }
            _geminiChat.value = _geminiChat.value + ChatMessage(aiProvider, response)
            isGeminiGenerating = false
        }
    }

    fun clearChat() {
        _geminiChat.value = listOf(
            ChatMessage("system", if (isVietnamese) "Đã dọn dẹp lịch sử trò chuyện. Editor Monaco sẵn sàng cho các lệnh mới!" else "Cleared chat history. Monaco Editor is ready for new interactions!")
        )
    }

    // AI Auto-Coder Engine to write Code directly at Caret position
    fun generateAndInsertCode(onComplete: (String) -> Unit) {
        val promptText = aiAutoCoderPrompt.trim()
        if (promptText.isEmpty()) return
        isAiAutoCoderGenerating = true
        viewModelScope.launch {
            try {
                val systemInstruction = if (isVietnamese) {
                    "Bạn là hệ thống Auto-Coder (Code Generator) được giới hạn chỉ có khả năng viết code. Ngôn ngữ: $editorLanguage. QUY TẮC HIỆN HÀNH:\n" +
                            "1. CHỈ TRẢ VỀ CHÍNH XÁC MÃ NGUỒN.\n" +
                            "2. KHÔNG BAO BỌC TRONG ``` HAY BẤT CỨ KÝ TỰ MARKDOWN NÀO.\n" +
                            "3. TUYỆT ĐỐI KHÔNG CHÀO HỎI HAY GIẢI THÍCH, ĐI THẲNG VÀO CODE.\n" +
                            "4. Chỉ sinh code đúng chuẩn, tối ưu và chạy được ngay."
                } else {
                    "You are an Auto-Coder (Code Generator) system limited only to writing code. Language: $editorLanguage. CURRENT RULES:\n" +
                            "1. ONLY RETURN THE EXACT SOURCE CODE.\n" +
                            "2. DO NOT WRAP IN ``` OR ANY MARKDOWN FORMATS.\n" +
                            "3. NEVER CONGRATULATE, GREET OR EXPLAIN, GO STRAIGHT INTO CODE.\n" +
                            "4. Only generate correct, optimized, immediately runnable code."
                }
                val response = GeminiRetrofitClient.generateContent(
                    prompt = promptText,
                    systemPrompt = systemInstruction,
                    model = activeAiModel,
                    customApiKey = geminiApiKey,
                    isViet = isVietnamese
                )
                val isErr = response.startsWith("Lỗi") || response.startsWith("VUI LÒNG") ||
                        response.startsWith("Error") || response.startsWith("PLEASE")
                if (isErr) {
                    val label = if (isVietnamese) "LỖI TỪ AI:" else "ERROR FROM AI:"
                    val errorPrefix = when (editorLanguage.lowercase()) {
                        "html" -> "<!--\n $label\n"
                        "python", "ruby" -> "\"\"\"\n $label\n"
                        else -> "/*\n $label\n"
                    }
                    val errorSuffix = when (editorLanguage.lowercase()) {
                        "html" -> "\n-->"
                        "python", "ruby" -> "\n\"\"\""
                        else -> "\n*/"
                    }
                    onComplete("$errorPrefix$response$errorSuffix\n")
                    return@launch
                }

                // Scrub standard Markdown formatting block lines if model makes mistakes
                var resultText = response.trim()
                if (resultText.startsWith("```")) {
                    val lines = resultText.split("\n")
                    if (lines.size > 2) {
                        resultText = lines.subList(1, lines.size - 1).joinToString("\n")
                    } else {
                        resultText = resultText.replace("```", "")
                    }
                }
                resultText = resultText.replace("```" + editorLanguage, "").replace("```", "").trim()
                onComplete(resultText)
            } catch (e: Exception) {
                val errorMsg = if (isVietnamese) "Lỗi hệ thống AI Auto-Coder: ${e.message}" else "AI Auto-Coder system error: ${e.message}"
                val label = if (isVietnamese) "LỖI TỪ AI:" else "ERROR FROM AI:"
                val errorPrefix = when (editorLanguage.lowercase()) {
                    "html" -> "<!--\n $label\n"
                    "python", "ruby" -> "\"\"\"\n $label\n"
                    else -> "/*\n $label\n"
                }
                val errorSuffix = when (editorLanguage.lowercase()) {
                    "html" -> "\n-->"
                    "python", "ruby" -> "\n\"\"\""
                    else -> "\n*/"
                }
                onComplete("$errorPrefix$errorMsg$errorSuffix\n")
            } finally {
                aiAutoCoderPrompt = ""
                isAiAutoCoderGenerating = false
            }
        }
    }

    // Local snippets/gists management
    fun saveSnippetFromEditor() {
        val text = editorText
        if (text.trim().isEmpty()) return
        val title = activeFile?.name ?: "untitled"

        viewModelScope.launch {
            repository.insertSnippet(
                SnippetEntity(
                    title = title,
                    content = text,
                    language = editorLanguage
                )
            )
        }
    }

    fun loadSnippetIntoEditor(snippet: SnippetEntity) {
        val proj = currentProject ?: return
        viewModelScope.launch {
            val newFile = FileEntity(
                projectId = proj.id,
                name = if (snippet.title.contains(".")) snippet.title else "${snippet.title}.txt",
                path = "snippets/${snippet.title}",
                isFolder = false,
                content = snippet.content,
                language = snippet.language
            )
            val id = repository.insertFile(newFile)
            val fileWithId = newFile.copy(id = id)
            setFileActive(fileWithId)
            activeTab = SidebarTab.FILES
        }
    }

    fun deleteSnippet(snippet: SnippetEntity) {
        viewModelScope.launch {
            repository.deleteSnippet(snippet.id)
        }
    }

    private fun detectLanguage(fileName: String): String {
        return when {
            fileName.endsWith(".kt") -> "kotlin"
            fileName.endsWith(".java") -> "java"
            fileName.endsWith(".js") -> "javascript"
            fileName.endsWith(".ts") -> "typescript"
            fileName.endsWith(".py") -> "python"
            fileName.endsWith(".html") || fileName.endsWith(".htm") -> "html"
            fileName.endsWith(".css") -> "css"
            fileName.endsWith(".json") -> "json"
            fileName.endsWith(".md") -> "markdown"
            else -> "plaintext"
        }
    }
}
