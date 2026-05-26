package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
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

@Composable
fun SnippetPanel(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val themeColors = ThemeRegistry.getTheme(viewModel.editorTheme)
    val snippets by viewModel.allSnippets.collectAsState()
    val isViet = viewModel.isVietnamese

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bookmarks,
                contentDescription = null,
                tint = Color(0xFF2EA043),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isViet) "THƯ VIỆN SNIPPETS" else "SNIPPETS LIBRARY",
                style = MaterialTheme.typography.titleSmall,
                color = themeColors.text,
                fontWeight = FontWeight.Bold
            )
        }

        Divider(color = Color.DarkGray.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp))

        if (snippets.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Bookmarks,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isViet) "Không có Snippet đã lưu" else "No saved snippets",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (isViet) "Nhấn biểu tượng Bookmarks trong Editor để lưu lại những dòng code tâm đắc!" else "Tap the Bookmarks icon in the Editor to save your favorite snippets!",
                        color = Color.Gray.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(snippets) { snippet ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = themeColors.headerBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.loadSnippetIntoEditor(snippet) }
                            ) {
                                Text(
                                    text = snippet.title,
                                    color = themeColors.text,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (isViet) "Ngôn ngữ: ${snippet.language.uppercase()}" else "Language: ${snippet.language.uppercase()}",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            IconButton(onClick = { viewModel.deleteSnippet(snippet) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = if (isViet) "Xóa Snippet" else "Delete Snippet",
                                    tint = Color(0xFFF85149),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
