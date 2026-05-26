package com.example.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.viewmodel.EditorViewModel
import java.io.File

@Composable
fun WebPreview(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val activeFile = viewModel.activeFile
    val codeText = viewModel.editorText
    val isViet = viewModel.isVietnamese
    val projectFiles by viewModel.projectFiles.collectAsState()
    val themeColors = ThemeRegistry.getTheme(viewModel.editorTheme)
    
    val context = LocalContext.current
    val previewDir = remember { File(context.cacheDir, "web_preview_workspace") }
    
    var webUrlToLoad by remember { mutableStateOf("") }
    var lastSyncCount by remember { mutableStateOf(0) }

    val syncWorkspace = {
        try {
            if (previewDir.exists()) {
                previewDir.deleteRecursively()
            }
            previewDir.mkdirs()
            
            projectFiles.forEach { file ->
                if (file.isFolder) {
                    File(previewDir, file.path).mkdirs()
                } else {
                    val destFile = File(previewDir, file.path)
                    destFile.parentFile?.mkdirs()
                    val textToWrite = if (file.id == activeFile?.id) codeText else file.content
                    destFile.writeText(textToWrite)
                }
            }
            lastSyncCount++
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Lỗi đồng bộ files: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(activeFile, codeText) {
        syncWorkspace()
        if (activeFile != null && !activeFile.isFolder) {
            val targetFile = File(previewDir, activeFile.path)
            
            if (activeFile.name.endsWith(".md")) {
                val mdContent = if (activeFile.id == activeFile?.id) codeText else targetFile.readText()
                val encodedHtml = android.net.Uri.encode(mdContent)
                val htmlData = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="utf-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                        <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
                        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/github-markdown-css/5.2.0/github-markdown.min.css">
                        <style>
                            body {
                                box-sizing: border-box;
                                min-width: 200px;
                                max-width: 980px;
                                margin: 0 auto;
                                padding: 45px;
                            }
                            @media (max-width: 767px) {
                                body { padding: 15px; }
                            }
                        </style>
                    </head>
                    <body class="markdown-body">
                        <div id="content"></div>
                        <script>
                            document.getElementById('content').innerHTML = marked.parse(decodeURIComponent("$encodedHtml"));
                        </script>
                    </body>
                    </html>
                """.trimIndent()
                val tempHtml = File(previewDir, "md_preview_temp.html")
                tempHtml.writeText(htmlData)
                webUrlToLoad = "file://" + tempHtml.absolutePath
            } else if (activeFile.name.endsWith(".html")) {
                webUrlToLoad = "file://" + targetFile.absolutePath
            } else {
                webUrlToLoad = ""
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(14.dp)
    ) {
        // Preview Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Html,
                    contentDescription = null,
                    tint = Color(0xFFE06C75),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isViet) "TRÌNH CHẠY WEBSITE DỰ ÁN" else "WEBSITE LIVE RUNNER",
                    style = MaterialTheme.typography.titleMedium,
                    color = themeColors.text,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = {
                    syncWorkspace()
                    Toast.makeText(context, if (isViet) "Đang nạp lại trang..." else "Reloading live page...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = Color(0xFF58A6FF)
                )
            }
        }

        // Main preview container or standby panel
        if (activeFile == null || !(activeFile.name.endsWith(".html") || activeFile.name.endsWith(".md"))) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Html,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isViet) "Đang chạy. Hãy mở tệp HTML/Markdown" else "Online. Waiting for HTML/Markdown file",
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (isViet) 
                            "Chọn tệp .html hoặc .md từ ngăn kéo bên trái để render trực tiếp" 
                            else "Select any .html or .md from the files sidebar drawer to display",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            if (webUrlToLoad.isNotEmpty()) {
                    key(webUrlToLoad, lastSyncCount) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    webViewClient = WebViewClient()
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.useWideViewPort = true
                                    settings.loadWithOverviewMode = true
                                    settings.allowFileAccess = true
                                    settings.allowContentAccess = true
                                    settings.allowFileAccessFromFileURLs = true
                                    settings.allowUniversalAccessFromFileURLs = true
                                    loadUrl(webUrlToLoad)
                                }
                            },
                            update = { view ->
                                view.loadUrl(webUrlToLoad)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .background(Color.White)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = themeColors.keyword)
                    }
                }
            }
        }
    }
