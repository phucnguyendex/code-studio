package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

enum class DiffType {
    UNCHANGED, INSERTED, DELETED
}

data class DiffLine(
    val type: DiffType,
    val text: String,
    val oldNo: Int?,
    val newNo: Int?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffPanel(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val activeFile = viewModel.activeFile
    val themeColors = ThemeRegistry.getTheme(viewModel.editorTheme)
    val isViet = viewModel.isVietnamese
    
    var compareMode by remember { mutableStateOf("original") } // "original" or "github"
    var githubFileUrl by remember { mutableStateOf("") }
    var fetchedGithubContent by remember { mutableStateOf<String?>(null) }
    var isFetching by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(16.dp)
    ) {
        // Render Header Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Compare,
                contentDescription = null,
                tint = themeColors.keyword,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isViet) "SO SÁNH THAY ĐỔI (GIT DIFF)" else "COMPARE CHANGES (GIT DIFF)",
                style = MaterialTheme.typography.titleSmall,
                color = themeColors.text,
                fontWeight = FontWeight.Bold
            )
        }

        if (activeFile == null) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isViet) "Vui lòng chọn một tệp trong dự án để so sánh." else "Please select a file to compare.",
                    color = themeColors.text.copy(alpha = 0.4f),
                    fontSize = 13.sp
                )
            }
            return@Column
        }

        // Segmented control to choose baseline
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val originalLabel = if (isViet) "Bản gốc khi tải" else "Original Snapshot"
            val githubLabel = if (isViet) "Bản online GitHub" else "GitHub Live Repo"
            
            FilterChip(
                selected = compareMode == "original",
                onClick = {
                    compareMode = "original"
                    statusMessage = null
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = themeColors.keyword.copy(alpha = 0.2f),
                    selectedLabelColor = themeColors.keyword
                ),
                label = { Text(originalLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            
            FilterChip(
                selected = compareMode == "github",
                onClick = {
                    compareMode = "github"
                    statusMessage = null
                    // Set up initial raw github link if possible
                    if (viewModel.githubUsername.isNotEmpty() && viewModel.currentProject != null) {
                        githubFileUrl = "https://raw.githubusercontent.com/${viewModel.githubUsername}/${viewModel.currentProject!!.name}/main/${activeFile.path}"
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = themeColors.keyword.copy(alpha = 0.2f),
                    selectedLabelColor = themeColors.keyword
                ),
                label = { Text(githubLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        // If Compare with GitHub online chosen, offer url fetch config
        if (compareMode == "github") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.headerBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = if (isViet) "ĐƯỜNG DẪN RAW FILE TRÊN GITHUB" else "GITHUB RAW FILE ABSOLUTE URL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.keyword
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = githubFileUrl,
                            onValueChange = { githubFileUrl = it },
                            placeholder = { Text("https://raw.githubusercontent.com/...", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = themeColors.text,
                                unfocusedTextColor = themeColors.text,
                                focusedBorderColor = themeColors.keyword,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                isFetching = true
                                statusMessage = null
                                coroutineScope.launch {
                                    try {
                                        val content = withContext(Dispatchers.IO) {
                                            URL(githubFileUrl).readText()
                                        }
                                        fetchedGithubContent = content
                                        statusMessage = if (isViet) "Đã đồng bộ live repo thành công!" else "Synced GitHub live repo successfully!"
                                    } catch (e: Exception) {
                                        statusMessage = (if (isViet) "Lỗi kết nối: " else "Fetch failed: ") + e.localizedMessage
                                    } finally {
                                        isFetching = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F52BA)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            if (isFetching) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp))
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isViet) "Tải" else "Fetch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    statusMessage?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            fontSize = 10.sp,
                            color = if (it.contains("Lỗi") || it.contains("fail")) Color(0xFFF85149) else Color(0xFF2EA043),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        val originalText = remember(activeFile.id, compareMode, fetchedGithubContent) {
            if (compareMode == "github") {
                fetchedGithubContent ?: ""
            } else {
                viewModel.getOriginalContent(activeFile.id)
            }
        }
        val currentText = viewModel.editorText

        val diffLines = remember(originalText, currentText) {
            val oldLines = originalText.split("\n")
            val newLines = currentText.split("\n")
            calculateDiff(oldLines, newLines)
        }

        val additionsCount = remember(diffLines) { diffLines.count { it.type == DiffType.INSERTED } }
        val deletionsCount = remember(diffLines) { diffLines.count { it.type == DiffType.DELETED } }

        // Top info bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            color = themeColors.headerBackground,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeFile.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.text,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF2EA043).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isViet) "+$additionsCount dòng thêm" else "+$additionsCount insertions",
                                color = Color(0xFF2EA0435),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFF85149).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isViet) "-$deletionsCount dòng xóa" else "-$deletionsCount deletions",
                                color = Color(0xFFF85149),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                // Actions config
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (additionsCount > 0 || deletionsCount > 0) {
                        FilledTonalButton(
                            onClick = {
                                viewModel.handleTextEdit(originalText)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFF85149).copy(alpha = 0.1f),
                                contentColor = Color(0xFFF85149)
                            )
                        ) {
                            Text(if (isViet) "Khôi phục" else "Revert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = {
                                viewModel.saveActiveFileChanges()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.keyword,
                                contentColor = Color.White
                            )
                        ) {
                            Text(if (isViet) "Lưu tệp" else "Save File", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Main diff list
        if (additionsCount == 0 && deletionsCount == 0) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(themeColors.headerBackground.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2EA043),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isViet) "Không có thay đổi nào so với bản gốc." else "No differences found with baseline.",
                        fontSize = 12.sp,
                        color = themeColors.text.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                color = themeColors.headerBackground.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray.copy(alpha = 0.15f))
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(diffLines) { item ->
                        val (bgColor, textColor, sign) = when (item.type) {
                            DiffType.INSERTED -> Triple(Color(0xFF2EA043).copy(alpha = 0.15f), Color(0xFF3FB950), "+")
                            DiffType.DELETED -> Triple(Color(0xFFF85149).copy(alpha = 0.15f), Color(0xFFF85149), "-")
                            DiffType.UNCHANGED -> Triple(Color.Transparent, themeColors.text.copy(alpha = 0.8f), " ")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor)
                                .padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Line numbers labels
                            Text(
                                text = item.oldNo?.toString()?.padStart(3, ' ') ?: "   ",
                                color = themeColors.lineNumbersText.copy(alpha = 0.4f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.width(28.dp).padding(start = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                            Text(
                                text = item.newNo?.toString()?.padStart(3, ' ') ?: "   ",
                                color = themeColors.lineNumbersText.copy(alpha = 0.4f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.width(28.dp).padding(horizontal = 2.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                            
                            // Edit direction indicator
                            Text(
                                text = sign,
                                color = textColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            // Unified code line scrollable container
                            val horizontalScrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(horizontalScrollState)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = item.text,
                                    color = textColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculateDiff(oldLines: List<String>, newLines: List<String>): List<DiffLine> {
    val m = oldLines.size
    val n = newLines.size
    val dp = Array(m + 1) { IntArray(n + 1) }
    
    for (i in 1..m) {
        for (j in 1..n) {
            if (oldLines[i - 1] == newLines[j - 1]) {
                dp[i][j] = dp[i - 1][j - 1] + 1
            } else {
                dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
            }
        }
    }

    val result = mutableListOf<DiffLine>()
    var i = m
    var j = n

    while (i > 0 || j > 0) {
        if (i > 0 && j > 0 && oldLines[i - 1] == newLines[j - 1]) {
            result.add(0, DiffLine(type = DiffType.UNCHANGED, text = oldLines[i - 1], oldNo = i, newNo = j))
            i--
            j--
        } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
            result.add(0, DiffLine(type = DiffType.INSERTED, text = newLines[j - 1], oldNo = null, newNo = j))
            j--
        } else {
            result.add(0, DiffLine(type = DiffType.DELETED, text = oldLines[i - 1], oldNo = i, newNo = null))
            i--
        }
    }
    return result
}
