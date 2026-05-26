package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.EditorViewModel

@Composable
fun WelcomeScreen(viewModel: EditorViewModel) {
    val isViet = viewModel.isVietnamese
    val themeColors = ThemeRegistry.getTheme(viewModel.editorTheme)

    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.85f),
            colors = CardDefaults.cardColors(containerColor = themeColors.headerBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isViet) "Chào mừng đến với Code Studio 2.0" else "Welcome to Code Studio 2.0",
                    style = MaterialTheme.typography.titleLarge,
                    color = themeColors.text,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = if (isViet) "Vui lòng thiết lập Workspace đầu tiên (Hồ sơ) của bạn." else "Please set up your first Workspace Profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.text.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isViet) "Tên Workspace (Bắt buộc)" else "Workspace Name (Required)", color = themeColors.text.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = themeColors.keyword) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = themeColors.text,
                        unfocusedTextColor = themeColors.text,
                        focusedBorderColor = themeColors.keyword,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(if (isViet) "Tên người dùng GitHub (Không bắt buộc)" else "GitHub Username (Optional)", color = themeColors.text.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = themeColors.keyword) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = themeColors.text,
                        unfocusedTextColor = themeColors.text,
                        focusedBorderColor = themeColors.keyword,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(if (isViet) "GitHub Token (Không bắt buộc)" else "GitHub Token (Optional)", color = themeColors.text.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = themeColors.keyword) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = themeColors.text,
                        unfocusedTextColor = themeColors.text,
                        focusedBorderColor = themeColors.keyword,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val finalName = name.ifBlank { "Developer" }
                        viewModel.completeFirstLaunch(finalName, username, token)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.keyword)
                ) {
                    Text(if (isViet) "Bắt Đầu Code" else "Start Coding", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}
