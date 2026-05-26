package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

@Composable
fun GeminiPanel(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val themeColors = ThemeRegistry.getTheme(viewModel.editorTheme)
    val messages by viewModel.geminiChat.collectAsState()
    val isGenerating = viewModel.isGeminiGenerating
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isViet = viewModel.isVietnamese

    // Auto scroll to bottom
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(12.dp)
    ) {
        // AI Header Options
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isViet) "TRỢ LÝ AI" else "AI ASSISTANT",
                    style = MaterialTheme.typography.titleSmall,
                    color = themeColors.text,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = { viewModel.clearChat() }) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = if (isViet) "Xóa lịch sử" else "Clear history",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(8.dp)).background(themeColors.headerBackground),
        ) {
            val isGemini = viewModel.aiProvider == "gemini"
            Box(
                modifier = Modifier.weight(1f)
                    .clickable { viewModel.aiProvider = "gemini" }
                    .background(if (isGemini) Color(0xFF388BFD) else Color.Transparent)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Gemini API", 
                    color = if (isGemini) Color.White else themeColors.text.copy(alpha=0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier.weight(1f)
                    .clickable { viewModel.aiProvider = "copilot" }
                    .background(if (!isGemini) Color(0xFF388BFD) else Color.Transparent)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Copilot (Free)", 
                    color = if (!isGemini) Color.White else themeColors.text.copy(alpha=0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Chat stream listing
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.sender == "user"
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) Color(0xFF0F52BA).copy(alpha = 0.3f) else themeColors.headerBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (isUser) (if (isViet) "Bạn" else "You") else {
                                            if (msg.sender == "copilot" || (msg.sender == "system" && viewModel.aiProvider == "copilot")) "Copilot" else "Gemini Builder"
                                        },
                                        color = if (isUser) Color(0xFF58A6FF) else if (msg.sender == "copilot" || (msg.sender == "system" && viewModel.aiProvider == "copilot")) Color(0xFF2EA043) else Color(0xFFFFD700),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    SelectionContainer {
                                        if (isUser) {
                                            Text(
                                                text = msg.content,
                                                color = Color(0xFFC9D1D9),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        } else {
                                            ChatMarkdownFormatter(msg.content, themeColors, isViet = isViet)
                                        }
                                    }
                                }
                    }
                }
            }
            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFFD700),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (viewModel.aiProvider == "copilot") {
                                if (isViet) "Copilot đang suy nghĩ..." else "Copilot is thinking..."
                            } else {
                                if (isViet) "Gemini đang suy nghĩ..." else "Gemini is thinking..."
                            },
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input send layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(themeColors.headerBackground)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = viewModel.geminiPrompt,
                onValueChange = { viewModel.geminiPrompt = it },
                textStyle = LocalTextStyle.current.copy(color = themeColors.text, fontSize = 14.sp),
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                decorationBox = { innerTextField ->
                    if (viewModel.geminiPrompt.isEmpty()) {
                        Text(text = if (isViet) "Hỏi AI: giải thích, code thử, sửa lỗi..." else "Ask AI: explain, prototype, fix bugs...", color = Color.Gray, fontSize = 13.sp)
                    }
                    innerTextField()
                }
            )

            IconButton(
                onClick = { viewModel.sendGeminiChatMessage() },
                enabled = !isGenerating && viewModel.geminiPrompt.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = if (isViet) "Gửi" else "Send",
                    tint = if (viewModel.geminiPrompt.isBlank() || isGenerating) Color.Gray else Color(0xFF58A6FF)
                )
            }
        }
    }
}

@Composable
fun ChatMarkdownFormatter(content: String, themeColors: ThemeColors, isViet: Boolean) {
    val segments = parseMarkdownFragments(content)
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var copiedIndex by remember { mutableStateOf(-1) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        segments.forEachIndexed { index, segment ->
            when (segment) {
                is MarkdownSegment.TextSeq -> {
                    Text(
                        text = segment.text,
                        color = Color(0xFFC9D1D9),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is MarkdownSegment.CodeSeq -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(themeColors.headerBackground)
                            .padding(top = 8.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = segment.language.ifEmpty { "code" },
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.buildAnnotatedString { append(segment.code) })
                                        copiedIndex = index
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                                ) {
                                    Text(
                                        text = if (copiedIndex == index) (if (isViet) "Đã copy" else "Copied") else "Copy",
                                        color = if (copiedIndex == index) Color(0xFF4ADE80) else Color(0xFF58A6FF),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(themeColors.background)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = segment.code,
                                    color = Color(0xFFE5E5E5),
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    LaunchedEffect(copiedIndex) {
        if (copiedIndex != -1) {
            kotlinx.coroutines.delay(2000)
            copiedIndex = -1
        }
    }
}

sealed class MarkdownSegment {
    data class TextSeq(val text: String) : MarkdownSegment()
    data class CodeSeq(val code: String, val language: String) : MarkdownSegment()
}

fun parseMarkdownFragments(message: String): List<MarkdownSegment> {
    val segments = mutableListOf<MarkdownSegment>()
    val regex = Regex("```(\\w*)\\n(.*?)\\n?```", RegexOption.DOT_MATCHES_ALL)
    var lastIndex = 0
    
    regex.findAll(message).forEach { match ->
        val textBefore = message.substring(lastIndex, match.range.first)
        if (textBefore.trim().isNotEmpty()) {
            segments.add(MarkdownSegment.TextSeq(textBefore.trim()))
        }
        segments.add(MarkdownSegment.CodeSeq(match.groupValues[2].trimEnd(), match.groupValues[1]))
        lastIndex = match.range.last + 1
    }
    
    val remaining = message.substring(lastIndex)
    if (remaining.trim().isNotEmpty()) {
        segments.add(MarkdownSegment.TextSeq(remaining.trim()))
    }
    
    return segments
}
