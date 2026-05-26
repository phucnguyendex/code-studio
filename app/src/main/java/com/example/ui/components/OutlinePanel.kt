package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.EditorViewModel
import java.util.regex.Pattern

data class ParsedSymbol(
    val name: String,
    val type: String, // "class", "function", "variable", "tag", "rule"
    val icon: ImageVector,
    val color: Color,
    val lineNumber: Int,
    val preview: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinePanel(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val activeFile = viewModel.activeFile
    val codeText = viewModel.editorText
    val themeColors = ThemeRegistry.getTheme(viewModel.editorTheme)
    var searchQuery by remember { mutableStateOf("") }
    val isViet = viewModel.isVietnamese

    val symbols = remember(codeText, activeFile?.language) {
        if (codeText.isEmpty() || activeFile == null) emptyList()
        else parseSymbols(codeText, viewModel.editorLanguage)
    }

    val filteredSymbols = remember(symbols, searchQuery) {
        if (searchQuery.trim().isEmpty()) symbols
        else symbols.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountTree,
                contentDescription = null,
                tint = themeColors.keyword,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isViet) "KÝ HIỆU & CẤU TRÚC (OUTLINE)" else "SYMBOLS & OUTLINE",
                style = MaterialTheme.typography.titleSmall,
                color = themeColors.text,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = if (isViet) "Nhấp đúp chuột hoặc chạm vào để định vị con trỏ nhanh tới dòng khai báo tương ứng trong Monaco Editor." else "Double click or tap to quickly position cursor to the corresponding declaration line in Monaco Editor.",
            color = themeColors.text.copy(alpha = 0.4f),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Filter text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (isViet) "Tìm nhanh ký hiệu..." else "Quick-search symbols...", fontSize = 13.sp, color = themeColors.text.copy(alpha = 0.4f)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColors.keyword,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = themeColors.text,
                unfocusedTextColor = themeColors.text
            ),
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = themeColors.text.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        Divider(color = Color.DarkGray.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 8.dp))

        if (activeFile == null) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isViet) "Không có tệp hoạt động nào được chọn." else "No active file selected.",
                    color = themeColors.text.copy(alpha = 0.4f),
                    fontSize = 13.sp
                )
            }
        } else if (filteredSymbols.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BubbleChart,
                        contentDescription = null,
                        tint = themeColors.text.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) {
                            if (isViet) "Không khớp kết quả tìm kiếm" else "No search results match"
                        } else {
                            if (isViet) "Không tìm thấy ký hiệu lớp hay hàm nào." else "No class or function symbols found."
                        },
                        color = themeColors.text.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredSymbols) { symbol ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .background(themeColors.headerBackground.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .clickable {
                                // Scroll and position caret
                                viewModel.requestGoToLine = symbol.lineNumber
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = symbol.icon,
                                contentDescription = symbol.type,
                                tint = symbol.color,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = symbol.name,
                                    fontSize = 13.sp,
                                    color = themeColors.text,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                                if (symbol.preview.isNotEmpty()) {
                                    Text(
                                        text = symbol.preview,
                                        fontSize = 10.sp,
                                        color = themeColors.text.copy(alpha = 0.5f),
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isViet) "Dòng ${symbol.lineNumber}" else "Line ${symbol.lineNumber}",
                            fontSize = 10.sp,
                            color = themeColors.keyword.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

private fun parseSymbols(code: String, lang: String): List<ParsedSymbol> {
    val result = mutableListOf<ParsedSymbol>()
    val lines = code.split("\n")
    val lowerLang = lang.lowercase()

    for (i in lines.indices) {
        val line = lines[i].trim()
        if (line.isEmpty() || line.startsWith("//") || line.startsWith("/*") || line.startsWith("*") || line.startsWith("#")) continue

        when {
            lowerLang == "kotlin" || lowerLang == "java" -> {
                // Class and Interfaces
                val classMatcher = Pattern.compile("\\b(?:class|interface|object)\\s+([a-zA-Z0-9_]+)").matcher(line)
                if (classMatcher.find()) {
                    result.add(
                        ParsedSymbol(
                            name = classMatcher.group(1) ?: "",
                            type = "class",
                            icon = Icons.Default.Token,
                            color = Color(0xFF4EC9B0),
                            lineNumber = i + 1,
                            preview = line
                        )
                    )
                    continue
                }

                // Functions
                val funPattern = if (lowerLang == "kotlin") {
                    Pattern.compile("\\bfun\\s+([a-zA-Z0-9_]+)")
                } else {
                    Pattern.compile("\\b(?:public|private|protected|static|void)\\s+(?:[a-zA-Z0-9_<>]+)\\s+([a-zA-Z0-9_]+)\\s*\\(")
                }
                val funMatcher = funPattern.matcher(line)
                if (funMatcher.find()) {
                    result.add(
                        ParsedSymbol(
                            name = funMatcher.group(1) ?: "",
                            type = "function",
                            icon = Icons.Default.DataObject,
                            color = Color(0xFFDCDCAA),
                            lineNumber = i + 1,
                            preview = line
                        )
                    )
                    continue
                }

                // Variables / Constants
                val valMatcher = Pattern.compile("\\b(?:val|var|const|int|String|Double|float|boolean)\\s+([a-zA-Z0-9_]+)").matcher(line)
                if (valMatcher.find()) {
                    result.add(
                        ParsedSymbol(
                            name = valMatcher.group(1) ?: "",
                            type = "variable",
                            icon = Icons.Default.DriveFileRenameOutline,
                            color = Color(0xFF9CDCFE),
                            lineNumber = i + 1,
                            preview = line
                        )
                    )
                }
            }

            lowerLang == "html" || lowerLang == "xml" -> {
                // Tags with IDs or classes
                val tagMatcher = Pattern.compile("<([a-zA-Z0-9-]+)(?:[^>]*)(?:id|class)=\"([^\"]+)\"").matcher(line)
                if (tagMatcher.find()) {
                    result.add(
                        ParsedSymbol(
                            name = "<${tagMatcher.group(1)}> (${tagMatcher.group(2)})",
                            type = "tag",
                            icon = Icons.Default.Html,
                            color = Color(0xFFFF7B72),
                            lineNumber = i + 1,
                            preview = line
                        )
                    )
                }
            }

            lowerLang == "css" || lowerLang == "scss" -> {
                // Selectors
                val cssMatcher = Pattern.compile("([.#][a-zA-Z0-9_-]+)\\s*\\{").matcher(line)
                if (cssMatcher.find()) {
                    result.add(
                        ParsedSymbol(
                            name = cssMatcher.group(1) ?: "",
                            type = "rule",
                            icon = Icons.Default.Style,
                            color = Color(0xFFCE9178),
                            lineNumber = i + 1,
                            preview = line
                        )
                    )
                }
            }

            lowerLang == "javascript" || lowerLang == "typescript" -> {
                // Function and Consts declarations
                val jsMatcher = Pattern.compile("\\b(?:function|class)\\s+([a-zA-Z0-9_]+)").matcher(line)
                if (jsMatcher.find()) {
                    result.add(
                        ParsedSymbol(
                            name = jsMatcher.group(1) ?: "",
                            type = "class",
                            icon = Icons.Default.DataObject,
                            color = Color(0xFFDCDCAA),
                            lineNumber = i + 1,
                            preview = line
                        )
                    )
                    continue
                }

                val constMatcher = Pattern.compile("\\b(?:const|let|var)\\s+([a-zA-Z0-9_]+)\\s*=").matcher(line)
                if (constMatcher.find()) {
                    result.add(
                        ParsedSymbol(
                            name = constMatcher.group(1) ?: "",
                            type = "variable",
                            icon = Icons.Default.DriveFileRenameOutline,
                            color = Color(0xFF9CDCFE),
                            lineNumber = i + 1,
                            preview = line
                        )
                    )
                }
            }

            lowerLang == "python" -> {
                val pyMatcher = Pattern.compile("\\b(?:def|class)\\s+([a-zA-Z0-9_]+)").matcher(line)
                if (pyMatcher.find()) {
                    val isDef = line.contains("def")
                    result.add(
                        ParsedSymbol(
                            name = pyMatcher.group(1) ?: "",
                            type = if (isDef) "function" else "class",
                            icon = if (isDef) Icons.Default.DataObject else Icons.Default.Token,
                            color = if (isDef) Color(0xFFDCDCAA) else Color(0xFF4EC9B0),
                            lineNumber = i + 1,
                            preview = line
                        )
                    )
                }
            }
        }
    }
    return result
}
