package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.EditorViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    var tokenInput by remember { mutableStateOf(viewModel.githubToken) }
    var usernameInput by remember { mutableStateOf(viewModel.githubUsername) }
    var geminiKeyInput by remember { mutableStateOf(viewModel.geminiApiKey) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var localFontSize by remember(viewModel.fontSize) { mutableStateOf(viewModel.fontSize.toFloat()) }
    
    val isViet = viewModel.isVietnamese
    val themeColors = ThemeRegistry.getTheme(viewModel.editorTheme)
    val context = androidx.compose.ui.platform.LocalContext.current

    var jsonConfigText by remember { mutableStateOf("") }
    var isImportExportExpanded by remember { mutableStateOf(false) }

    val createDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(jsonConfigText.toByteArray())
                }
                android.widget.Toast.makeText(context, if (isViet) "Đã xuất cấu hình ra file!" else "Exported config to file!", android.widget.Toast.LENGTH_SHORT).show()
                isImportExportExpanded = false
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, if (isViet) "Lỗi xuất file: ${e.message}" else "Export error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val openDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().use { it.readText() }
                    
                    val obj = org.json.JSONObject(text)
                    val token = obj.optString("github_token", "")
                    val username = obj.optString("github_username", "")
                    val geminiKey = obj.optString("gemini_api_key", "")
                    
                    if (obj.has("profile_name") || obj.has("github_username") || obj.has("github_token")) {
                        viewModel.completeFirstLaunch(
                            obj.optString("profile_name", viewModel.profileName),
                            obj.optString("github_username", viewModel.githubUsername),
                            obj.optString("github_token", viewModel.githubToken)
                        )
                    }
                    if (obj.has("has_app_lock") || obj.has("app_lock_pin")) {
                        viewModel.saveAppLock(
                            obj.optBoolean("has_app_lock", viewModel.hasAppLock),
                            obj.optString("app_lock_pin", viewModel.appLockPin)
                        )
                    }
                    if (obj.has("is_first_launch")) {
                        val firstLaunch = obj.getBoolean("is_first_launch")
                        viewModel.isFirstLaunch = firstLaunch
                        // Need to update prefs directly
                        val prefs = context.getSharedPreferences("monaco_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("is_first_launch", firstLaunch).apply()
                    }

                    if (geminiKey.isNotEmpty()) viewModel.saveGeminiApiKey(geminiKey)
                    
                    if (obj.has("editor_theme")) viewModel.editorTheme = obj.getString("editor_theme")
                    if (obj.has("editor_font_size")) viewModel.fontSize = obj.getInt("editor_font_size")
                    if (obj.has("auto_highlight")) viewModel.runAutoHighlight = obj.getBoolean("auto_highlight")
                    if (obj.has("is_vietnamese")) viewModel.isVietnamese = obj.getBoolean("is_vietnamese")

                    android.widget.Toast.makeText(context, if (isViet) "Đã phục hồi cấu hình từ file!" else "Config restored from file!", android.widget.Toast.LENGTH_SHORT).show()
                    isImportExportExpanded = false
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, if (isViet) "Lỗi cấu hình: ${e.message}" else "Config error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Settings general label header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = themeColors.keyword,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isViet) "THIẾT LẬP APP" else "APP SETTINGS",
                style = MaterialTheme.typography.titleSmall,
                color = themeColors.text,
                fontWeight = FontWeight.Bold
            )
        }

        Divider(color = Color.DarkGray.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 16.dp))

        // Language Switcher Toggle
        Text(
            text = if (isViet) "NGÔN NGỮ / LANGUAGE" else "LANGUAGE",
            color = themeColors.text.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = isViet,
                onClick = { viewModel.toggleLanguage(true) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = themeColors.keyword.copy(alpha = 0.2f),
                    selectedLabelColor = themeColors.keyword,
                    containerColor = themeColors.headerBackground,
                    labelColor = themeColors.text.copy(alpha = 0.7f)
                ),
                label = { Text("Tiếng Việt 🇻🇳", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            FilterChip(
                selected = !isViet,
                onClick = { viewModel.toggleLanguage(false) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = themeColors.keyword.copy(alpha = 0.2f),
                    selectedLabelColor = themeColors.keyword,
                    containerColor = themeColors.headerBackground,
                    labelColor = themeColors.text.copy(alpha = 0.7f)
                ),
                label = { Text("English 🇬🇧", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        // Theme selection
        Text(
            text = if (isViet) "GIAO DIỆN CHỦ ĐỀ" else "EDITOR THEME",
            color = themeColors.text.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            com.example.ui.components.ThemeRegistry.themes.keys.forEach { theme ->
                FilterChip(
                    selected = viewModel.editorTheme == theme,
                    onClick = { viewModel.saveEditorTheme(theme) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = themeColors.keyword.copy(alpha = 0.2f),
                        selectedLabelColor = themeColors.keyword,
                        containerColor = themeColors.headerBackground,
                        labelColor = themeColors.text.copy(alpha = 0.7f)
                    ),
                    label = { Text(theme, fontSize = 11.sp) }
                )
            }
        }

        // Editor Font Size Preference Selection
        Text(
            text = if (isViet) "KÍCH THƯỚC CHỮ" else "FONT SIZE",
            color = themeColors.text.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = localFontSize,
                onValueChange = { localFontSize = it },
                onValueChangeFinished = {
                    val finalSize = localFontSize.toInt().coerceIn(1, 20)
                    viewModel.saveFontSize(finalSize)
                },
                valueRange = 1f..20f,
                steps = 18,
                colors = SliderDefaults.colors(
                    thumbColor = themeColors.keyword,
                    activeTrackColor = themeColors.keyword,
                    inactiveTrackColor = themeColors.text.copy(alpha = 0.2f)
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${localFontSize.toInt().coerceIn(1, 20)}px",
                color = themeColors.keyword,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Auto Highlighting switch preference
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isViet) "NỔI BẬT CÚ PHÁP" else "SYNTAX HIGHLIGHTING",
                    color = themeColors.text.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isViet) "Tô màu mã nguồn cục bộ." else "Colorize code syntax based on language.",
                    color = themeColors.text.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }

            Switch(
                checked = viewModel.runAutoHighlight,
                onCheckedChange = { viewModel.saveAutoHighlight(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = themeColors.keyword,
                    checkedTrackColor = themeColors.keyword.copy(alpha = 0.5f)
                )
            )
        }

        Divider(color = themeColors.text.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))

        // Profile & Security
        Text(
            text = if (isViet) "BẢO MẬT WORKSPACE" else "WORKSPACE SECURITY",
            color = themeColors.text.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isViet) "Khóa ứng dụng bằng mã PIN" else "Require PIN to open app",
                color = themeColors.text,
                fontSize = 13.sp
            )
            Switch(
                checked = viewModel.hasAppLock,
                onCheckedChange = { 
                    viewModel.saveAppLock(it, if (!it) "" else viewModel.appLockPin)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = themeColors.keyword,
                    checkedTrackColor = themeColors.keyword.copy(alpha = 0.5f)
                )
            )
        }

        if (viewModel.hasAppLock) {
            OutlinedTextField(
                value = viewModel.appLockPin,
                onValueChange = { 
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        viewModel.saveAppLock(true, it)
                    }
                },
                label = { Text(if (isViet) "Mã PIN (4-6 số)" else "PIN Code (4-6 digits)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = themeColors.text.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = themeColors.text,
                    unfocusedTextColor = themeColors.text,
                    focusedBorderColor = themeColors.keyword,
                    unfocusedBorderColor = Color.DarkGray
                ),
                singleLine = true
            )
        }

        var showAdvanced by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { showAdvanced = !showAdvanced },
            colors = CardDefaults.cardColors(containerColor = themeColors.keyword.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (isViet) "Tích hợp nâng cao" else "Advanced Integrations", color = themeColors.keyword, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Icon(
                    imageVector = if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = themeColors.keyword
                )
            }
        }

        if (showAdvanced) {
            // Dynamic Gemini API Key configuration
        Text(
            text = if (isViet) "GEMINI API KEY" else "GEMINI API KEY",
            color = themeColors.text.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = if (isViet) "Sử dụng key để dùng tính năng AI." else "Required for AI assistant.",
            color = themeColors.text.copy(alpha = 0.4f),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = geminiKeyInput,
            onValueChange = {
                geminiKeyInput = it
                viewModel.saveGeminiApiKey(it)
            },
            label = { Text(if (isViet) "Nhập Gemini API Key..." else "Enter Gemini API Key...", color = themeColors.text.copy(alpha = 0.5f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = themeColors.text,
                unfocusedTextColor = themeColors.text
            ),
            visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                    Icon(
                        imageVector = if (showGeminiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Show Key",
                        tint = themeColors.text.copy(alpha = 0.5f)
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        // GitHub Username configuration
        Text(
            text = if (isViet) "GITHUB USERNAME" else "GITHUB USERNAME",
            color = themeColors.text.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = if (isViet) "Tra cứu kho lưu trữ trên xa." else "Explore remote Github repositories.",
            color = themeColors.text.copy(alpha = 0.4f),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = usernameInput,
            onValueChange = {
                usernameInput = it
                viewModel.saveGithubUsername(it)
            },
            label = { Text(if (isViet) "Nhập GitHub Username..." else "Specify Github Username...", color = themeColors.text.copy(alpha = 0.5f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColors.keyword,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = themeColors.text,
                unfocusedTextColor = themeColors.text
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        // GitHub Token configuration
        Text(
            text = if (isViet) "GITHUB PERSONAL ACCESS TOKEN" else "GITHUB PERSONAL ACCESS TOKEN",
            color = themeColors.text.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = if (isViet) "Tăng giới hạn API và cho phép git push." else "Enhance API rate limits and allow push.",
            color = themeColors.text.copy(alpha = 0.4f),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = tokenInput,
            onValueChange = {
                tokenInput = it
                viewModel.saveGithubToken(it)
            },
            label = { Text(if (isViet) "Nhập Token (ghp_...)" else "Enter Token (ghp_...)", color = themeColors.text.copy(alpha = 0.5f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColors.keyword,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = themeColors.text,
                unfocusedTextColor = themeColors.text
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        )

        // Backup manager
        Text(
            text = if (isViet) "SAO LƯU DỮ LIỆU" else "DATA BACKUP",
            color = themeColors.text.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = if (isViet) "Xuất/nhập hồ sơ và dự án (JSON)." else "Export/Import profiles and setup (JSON).",
            color = themeColors.text.copy(alpha = 0.4f),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    try {
                        val obj = org.json.JSONObject()
                        obj.put("profile_name", viewModel.profileName)
                        obj.put("has_app_lock", viewModel.hasAppLock)
                        obj.put("app_lock_pin", viewModel.appLockPin)
                        obj.put("is_first_launch", viewModel.isFirstLaunch)
                        obj.put("github_token", viewModel.githubToken)
                        obj.put("github_username", viewModel.githubUsername)
                        obj.put("gemini_api_key", viewModel.geminiApiKey)
                        obj.put("editor_theme", viewModel.editorTheme)
                        obj.put("editor_font_size", viewModel.fontSize)
                        obj.put("auto_highlight", viewModel.runAutoHighlight)
                        obj.put("is_vietnamese", viewModel.isVietnamese)
                        jsonConfigText = obj.toString(4)
                        createDocumentLauncher.launch("codestudio_backup.json")
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Lỗi tạo cấu hình", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.headerBackground)
            ) {
                Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp), tint = themeColors.keyword)
                Spacer(Modifier.width(4.dp))
                Text(if (isViet) "Xuất (Export)" else "Export", fontSize = 12.sp, color = themeColors.keyword)
            }
            Button(
                onClick = { openDocumentLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.headerBackground)
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp), tint = themeColors.keyword)
                Spacer(Modifier.width(4.dp))
                Text(if (isViet) "Nhập (Import)" else "Import", fontSize = 12.sp, color = themeColors.keyword)
            }
        }

        }

        // Cache & Data Settings Section
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isViet) "QUẢN TRỊ FILE & BỘ NHỚ" else "FILE & STORAGE MANAGEMENT",
            color = themeColors.text.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    try {
                        val cacheDirDeleted = context.cacheDir?.deleteRecursively() ?: false
                        val extCacheDirDeleted = context.externalCacheDir?.deleteRecursively() ?: false
                        if (cacheDirDeleted || extCacheDirDeleted) {
                            android.widget.Toast.makeText(
                                context,
                                if (isViet) "Đã xoá bộ nhớ đệm thành công!" else "Cache cleared cleanly!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                if (isViet) "Bộ nhớ đệm trống hoặc không xoá được." else "Cache is empty or could not be fully cleared.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(
                            context,
                            if (isViet) "Lỗi khi xoá bộ nhớ đệm: ${e.message}" else "Error clearing cache: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.headerBackground)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = themeColors.keyword
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isViet) "Xoá Bộ Nhớ Đệm" else "Clear Cache",
                    fontSize = 11.sp,
                    color = themeColors.keyword
                )
            }

            var showConfirmResetDialog by remember { mutableStateOf(false) }

            Button(
                onClick = { showConfirmResetDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isViet) "Xoá Tất Cả Dữ Liệu" else "Reset All Data",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            if (showConfirmResetDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmResetDialog = false },
                    title = {
                        Text(
                            text = if (isViet) "Xác Nhận Xóa Dữ Liệu?" else "Confirm Reset All Data?",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    text = {
                        Text(
                            text = if (isViet) {
                                "Hành động này sẽ xóa vĩnh viễn tất cả tệp tin, cơ sở dữ liệu, tài khoản đã cấu hình và đưa ứng dụng về trạng thái mới cài đặt ban đầu. Bạn có chắc chắn muốn tiếp tục?"
                            } else {
                                "This will permanently delete all workspace files, databases, preferences, and restore the app to its original newly installed state. Are you sure you want to proceed?"
                            },
                            color = themeColors.text
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showConfirmResetDialog = false
                                try {
                                    val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                                    if (am != null) {
                                        val succeeded = am.clearApplicationUserData()
                                        if (!succeeded) {
                                            // Fallback if system call fails
                                            context.getSharedPreferences("monaco_prefs", android.content.Context.MODE_PRIVATE).edit().clear().commit()
                                            context.filesDir?.deleteRecursively()
                                            context.cacheDir?.deleteRecursively()
                                            android.widget.Toast.makeText(
                                                context,
                                                if (isViet) "Không thể xoá tự động toàn bộ, đã xoá các file thủ công." else "Could not reset via system API, files deleted manually.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } else {
                                        // Fallback if ActivityManager not available
                                        context.getSharedPreferences("monaco_prefs", android.content.Context.MODE_PRIVATE).edit().clear().commit()
                                        context.filesDir?.deleteRecursively()
                                        context.cacheDir?.deleteRecursively()
                                    }
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(if (isViet) "Xác nhận và Xóa" else "Confirm Reset", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showConfirmResetDialog = false }
                        ) {
                            Text(if (isViet) "Hủy" else "Cancel", color = themeColors.text.copy(alpha = 0.7f))
                        }
                    },
                    containerColor = themeColors.headerBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Information Box with Author Credit
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.headerBackground)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = themeColors.keyword
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Code Studio v2.0",
                        color = themeColors.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isViet) "Tác giả: Nguyễn Hồng Diễm Phúc 🇻🇳" else "Author: Nguyễn Hồng Diễm Phúc 🇻🇳",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = if (isViet) "Trình biên tập mã nguồn di động siêu cấp tích hợp Trí tuệ Nhân tạo đa hệ sáng tạo." else "Supercharged developer text environment powered by advanced collaborative local preview rendering.",
                        color = themeColors.text.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Collapsible Privacy Policy Card
        var showPrivacy by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = themeColors.headerBackground),
            border = if (showPrivacy) BorderStroke(1.dp, themeColors.keyword.copy(alpha = 0.5f)) else null
        ) {
            Column(modifier = Modifier.clickable { showPrivacy = !showPrivacy }.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = themeColors.keyword
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isViet) "Chính sách bảo mật & Quyền riêng tư" else "Privacy & Security Policy",
                        color = themeColors.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1.0f)
                    )
                    Icon(
                        imageVector = if (showPrivacy) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = themeColors.text.copy(alpha = 0.6f)
                    )
                }
                if (showPrivacy) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = themeColors.text.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isViet) {
                            "CHÍNH SÁCH BẢO MẬT & QUYỀN RIÊNG TƯ\n\n" +
                            "Ứng dụng Code Studio cam kết bảo vệ toàn diện quyền tự do cá nhân và an toàn dữ liệu tuyệt đối của bạn.\n\n" +
                            "1. Dữ liệu cục bộ (Offline-First):\n" +
                            "Tất cả dự án, tệp tin nguồn và cấu hình cá nhân được lưu trữ hoàn toàn nội bộ và trực tiếp trên bộ nhớ an toàn của thiết bị thông qua hệ thống Room Database và SharedPreferences ngoại tuyến. Chúng tôi KHÔNG thu thập, KHÔNG lưu giữ hay truyền tải mã nguồn hay bất kỳ nội dung cá nhân nào của bạn lên bất kỳ máy chủ bên thứ ba nào.\n\n" +
                            "2. Bảo mật Khóa ứng dụng (PIN):\n" +
                            "Mã khóa PIN được mã hóa một chiều và lưu trữ hoàn toàn cục bộ trên thiết bị của bạn nhằm phục vụ kiểm soát bảo mật mở ứng dụng, bảo vệ môi trường viết mã riêng tư của bạn.\n\n" +
                            "3. Mã khóa API & Token truy cập:\n" +
                            "Khóa Gemini API và GitHub Personal Access Token do bạn cung cấp chỉ được lưu trữ an toàn trong vùng nhớ cục bộ và được gọi trực tiếp và thiết lập kết nối an toàn tới các máy chủ API chính thức (Google Gemini API / GitHub API) từ chính thiết bị của bạn. Không có bất kỳ máy chủ trung gian nào theo dõi hoặc thu thập các khóa nhạy cảm này.\n\n" +
                            "4. Quyền hạn ứng dụng:\n" +
                            "Ứng dụng chỉ yêu cầu quyền lưu trữ tệp tin (khi bạn tải xuống/xuất/nhập dữ liệu cấu hình) và kết nối mạng Internet nhằm phục vụ các tính năng bạn chủ động thực hiện (Git Pull/Push kho lưu trữ Github hoặc đối thoại câu lệnh cùng AI trợ lý Gemini).\n\n" +
                            "Cảm ơn bạn đã tin tưởng lựa chọn và sử dụng sản phẩm từ nhà phát triển!"
                        } else {
                            "PRIVACY & SECURITY POLICY\n\n" +
                            "Code Studio is deeply committed to protecting your personal freedom and absolute data security.\n\n" +
                            "1. Local Storage (Offline-First):\n" +
                            "All workflow projects, source files, and user preferences are processed and stored strictly offline on your own device using local Room Database storage and SharedPreferences. We NEVER collect, mirror, or transmit your code or personal files to any third-party databases or servers.\n\n" +
                            "2. Application Lock (PIN Protection):\n" +
                            "Your security PIN code is cryptographically hashed and verified only locally on your device to restrict unauthorized entry to your mobile editor environment.\n\n" +
                            "3. Key Credential Storage:\n" +
                            "Your customized Gemini API Keys and GitHub Personal Access Tokens are saved safely in your app's local storage and used directly to establish end-to-end encrypted connections to official endpoints (Google Gemini API & GitHub REST APIs) from your local device. No intermediate or monitoring servers are involved.\n\n" +
                            "4. Required Platform Permissions:\n" +
                            "The app requests workspace storage access (for configuration export/import operations) and basic internet permissions solely to process explicit user tasks (such as cloning GitHub repositories, syncing commits, or triggering generative responses via private Gemini API credentials).\n\n" +
                            "Thank you for trusting and utilizing our developer tools suite!"
                        },
                        color = themeColors.text.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Collapsible MIT License Card
        var showLicense by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = themeColors.headerBackground),
            border = if (showLicense) BorderStroke(1.dp, themeColors.keyword.copy(alpha = 0.5f)) else null
        ) {
            Column(modifier = Modifier.clickable { showLicense = !showLicense }.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = themeColors.keyword
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isViet) "Bản quyền & Giấy phép MIT" else "Copyright & MIT License",
                        color = themeColors.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1.0f)
                    )
                    Icon(
                        imageVector = if (showLicense) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = themeColors.text.copy(alpha = 0.6f)
                    )
                }
                if (showLicense) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = themeColors.text.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "BẢN QUYỀN © 2026 NGUYỄN HỒNG DIỄM PHÚC.\nĐÃ ĐĂNG KÝ BẢN QUYỀN.\n\n" +
                        "MIT LICENSE\n\n" +
                        "Copyright (c) 2026 Nguyễn Hồng Diễm Phúc\n\n" +
                        "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\n" +
                        "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\n" +
                        "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.",
                        color = themeColors.text.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}
