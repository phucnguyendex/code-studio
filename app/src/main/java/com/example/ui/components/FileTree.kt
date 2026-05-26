package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.FileEntity
import com.example.database.ProjectEntity
import com.example.viewmodel.EditorViewModel

class TreeNode(
    val name: String,
    val path: String,
    val isFolder: Boolean,
    val fileEntity: FileEntity? = null,
    val children: MutableList<TreeNode> = mutableListOf()
)

fun buildFileTree(files: List<FileEntity>): TreeNode {
    val root = TreeNode("Root", "", true)
    
    // Sort files to handle folders/files sequentially
    val sortedFiles = files.sortedWith(compareBy({ !it.isFolder }, { it.path.count { c -> c == '/' } }, { it.path }))
    
    for (file in sortedFiles) {
        val parts = file.path.split('/')
        var current = root
        var currentPath = ""
        
        for (i in 0 until parts.size) {
            val part = parts[i]
            if (part.isEmpty()) continue
            
            currentPath = if (currentPath.isEmpty()) part else "$currentPath/$part"
            val isLast = (i == parts.size - 1)
            
            var child = current.children.find { it.name == part && it.isFolder == (if (isLast) file.isFolder else true) }
            if (child == null) {
                child = TreeNode(
                    name = part,
                    path = currentPath,
                    isFolder = if (isLast) file.isFolder else true,
                    fileEntity = if (isLast) file else null
                )
                current.children.add(child)
            }
            current = child
        }
    }
    return root
}

class TreeDisplayItem(
    val node: TreeNode,
    val depth: Int,
    val isExpanded: Boolean
)

fun traverseTree(
    node: TreeNode,
    depth: Int,
    expandedState: Map<String, Boolean>,
    result: MutableList<TreeDisplayItem>
) {
    val sortedChildren = node.children.sortedWith(compareBy({ !it.isFolder }, { it.name.lowercase() }))
    for (child in sortedChildren) {
        val isExpanded = expandedState[child.path] ?: false
        result.add(TreeDisplayItem(child, depth, isExpanded))
        if (child.isFolder && isExpanded) {
            traverseTree(child, depth + 1, expandedState, result)
        }
    }
}

fun getFileNameFromUri(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

@Composable
fun FileTree(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val themeColors = ThemeRegistry.getTheme(viewModel.editorTheme)
    val context = LocalContext.current
    val isViet = viewModel.isVietnamese
    val projects by viewModel.allProjects.collectAsState()
    val files by viewModel.projectFiles.collectAsState()
    val currentProj = viewModel.currentProject
    val activeFile = viewModel.activeFile

    // Tree Structure States
    val expandedState = remember { mutableStateMapOf<String, Boolean>() }
    val fileTree = remember(files) { buildFileTree(files) }
    val traversedItems = remember(fileTree, expandedState.toMap()) {
        val itemsList = mutableListOf<TreeDisplayItem>()
        traverseTree(fileTree, 0, expandedState, itemsList)
        itemsList
    }

    // Dialog state controllers
    var showAddProjectDialog by remember { mutableStateOf(false) }
    var newProjName by remember { mutableStateOf("") }
    var newProjDesc by remember { mutableStateOf("") }
    var newGithubUser by remember { mutableStateOf("") }
    var newGithubToken by remember { mutableStateOf("") }

    var selectedTargetPath by remember { mutableStateOf("") }
    var showAddFileDialog by remember { mutableStateOf(false) }
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFolderName by remember { mutableStateOf("") }

    var showRenameDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<FileEntity?>(null) }
    var renameInputName by remember { mutableStateOf("") }

    var isProjectDropdownExpanded by remember { mutableStateOf(false) }

    // Upload launcher for device files
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val displayName = getFileNameFromUri(context, uri) ?: "upload_file.txt"
                val inputStream = context.contentResolver.openInputStream(uri)
                val textContent = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                
                // Save loaded file entity inside targets
                val finalPath = if (selectedTargetPath.isEmpty()) displayName else "$selectedTargetPath/$displayName"
                viewModel.addNewFileWithContent(displayName, finalPath, textContent)
                Toast.makeText(
                    context,
                    if (isViet) "Tải lên tệp thành công: $displayName" else "File uploaded successfully: $displayName",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (isViet) "Lỗi tải tệp lên: ${e.message}" else "Error uploading file: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(12.dp)
    ) {
        // Project selector title
        Text(
            text = if (isViet) "KHÔNG GIAN LÀM VIỆC" else "WORKSPACES",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isProjectDropdownExpanded = true },
            colors = CardDefaults.cardColors(containerColor = themeColors.headerBackground),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Workspaces,
                        contentDescription = null,
                        tint = Color(0xFF58A6FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentProj?.name ?: (if (isViet) "Chưa chọn dự án" else "No Workspace Active"),
                        color = themeColors.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.LightGray
                )
            }
        }

        DropdownMenu(
            expanded = isProjectDropdownExpanded,
            onDismissRequest = { isProjectDropdownExpanded = false },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(themeColors.headerBackground)
        ) {
            projects.forEach { proj ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = proj.name,
                            color = if (proj.id == currentProj?.id) Color(0xFF58A6FF) else Color.White
                        )
                    },
                    onClick = {
                        viewModel.selectProject(proj)
                        isProjectDropdownExpanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = if (proj.id == currentProj?.id) Color(0xFF58A6FF) else Color.LightGray
                        )
                    }
                )
            }
            Divider(color = Color.DarkGray)
            DropdownMenuItem(
                text = { Text(if (isViet) "Tạo không gian làm việc mới" else "Create new workspace", color = Color(0xFF2EA043)) },
                leadingIcon = { Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF2EA043)) },
                onClick = {
                    isProjectDropdownExpanded = false
                    showAddProjectDialog = true
                }
            )
            if (projects.size > 1) {
                DropdownMenuItem(
                    text = { Text(if (isViet) "Xóa không gian hiện tại" else "Delete current workspace", color = Color(0xFFF85149)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFF85149)) },
                    onClick = {
                        isProjectDropdownExpanded = false
                        viewModel.deleteCurrentProject()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Files List Header panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isViet) "TỆP TIN HỆ THỐNG (CÂY)" else "WORKSPACE FILE TREE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            // General action controls at the root folder
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Add File to Root
                IconButton(
                    onClick = {
                        selectedTargetPath = ""
                        showAddFileDialog = true
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAdd,
                        contentDescription = if (isViet) "Thêm tệp mới" else "Create file",
                        tint = Color(0xFF388BFD),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Add Folder to Root
                IconButton(
                    onClick = {
                        selectedTargetPath = ""
                        showAddFolderDialog = true
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = if (isViet) "Thêm thư mục mới" else "Create folder",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Upload file from device
                IconButton(
                    onClick = {
                        selectedTargetPath = ""
                        pickFileLauncher.launch("*/*")
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = if (isViet) "Tải lên từ thiết bị" else "Upload from device",
                        tint = Color(0xFF2EA043),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Document / Collapsible collapsible nested lists
        if (traversedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isViet) "Không gian trống, tạo tệp mới để bắt đầu" else "Workspace empty, create a file to start",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(traversedItems) { item ->
                    val node = item.node
                    val isFolder = node.isFolder
                    val depth = item.depth
                    val isExpanded = item.isExpanded

                    val fileIcon = when {
                        node.name.endsWith(".kt") || node.name.endsWith(".java") -> Icons.Default.Code
                        node.name.endsWith(".html") || node.name.endsWith(".css") -> Icons.Default.Html
                        node.name.endsWith(".md") -> Icons.Default.Book
                        else -> Icons.Default.Description
                    }

                    val isActive = !isFolder && node.fileEntity?.id == activeFile?.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) themeColors.headerBackground else Color.Transparent)
                            .clickable {
                                if (isFolder) {
                                    expandedState[node.path] = !isExpanded
                                } else {
                                    node.fileEntity?.let { viewModel.setFileActive(it) }
                                }
                            }
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Level Indentation Space block
                        Spacer(modifier = Modifier.width((depth * 14).dp))

                        // Folder Collapse / Expand chevron
                        if (isFolder) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.width(18.dp))
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // File type / Directory iconography
                        Icon(
                            imageVector = if (isFolder) {
                                if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder
                            } else {
                                fileIcon
                            },
                            contentDescription = null,
                            tint = when {
                                isFolder -> Color(0xFFE3B341)
                                isActive -> Color(0xFF58A6FF)
                                else -> Color.LightGray
                            },
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // File name textual display
                        Text(
                            text = node.name,
                            color = if (isActive) Color.White else Color(0xFFC9D1D9),
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Option operational quick elements
                        Row {
                            if (isFolder) {
                                // Add inner file under folder
                                IconButton(
                                    onClick = {
                                        selectedTargetPath = node.path
                                        showAddFileDialog = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NoteAdd,
                                        contentDescription = "New File in folder",
                                        tint = Color(0xFF388BFD).copy(alpha = 0.8f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Upload device file directly into this folder
                                IconButton(
                                    onClick = {
                                        selectedTargetPath = node.path
                                        pickFileLauncher.launch("*/*")
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = Color(0xFF2EA043).copy(alpha = 0.8f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else {
                                // Rename File element
                                IconButton(
                                    onClick = {
                                        fileToRename = node.fileEntity
                                        renameInputName = node.name
                                        showRenameDialog = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename file",
                                        tint = Color.LightGray.copy(alpha = 0.8f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // General Delete entity
                            IconButton(
                                onClick = {
                                    node.fileEntity?.let {
                                        viewModel.deleteFileEntity(it)
                                    } ?: run {
                                        // Delete implicitly built directory node, let's create structural FileEntity to resolve deletes or use ViewModel
                                        val dummyFile = FileEntity(
                                            id = 0,
                                            projectId = currentProj?.id ?: 0,
                                            name = node.name,
                                            path = node.path,
                                            isFolder = true
                                        )
                                        viewModel.deleteFileEntity(dummyFile)
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete item",
                                    tint = Color(0xFFF85149).copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Standard creation Dialogs ---
        if (showAddProjectDialog) {
            AlertDialog(
                onDismissRequest = { showAddProjectDialog = false },
                title = { Text(if (isViet) "Không gian làm việc mới" else "New Workspace", color = themeColors.text) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newProjName,
                            onValueChange = { newProjName = it },
                            label = { Text(if (isViet) "Tên Dự án" else "Project Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF388BFD),
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = themeColors.text,
                                unfocusedTextColor = themeColors.text
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newProjDesc,
                            onValueChange = { newProjDesc = it },
                            label = { Text(if (isViet) "Mô tả chi tiết" else "Detailed Description") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF388BFD),
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = themeColors.text,
                                unfocusedTextColor = themeColors.text
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newGithubUser,
                            onValueChange = { newGithubUser = it },
                            label = { Text(if (isViet) "GitHub Username (Tùy chọn)" else "GitHub Username (Optional)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF388BFD),
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = themeColors.text,
                                unfocusedTextColor = themeColors.text
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newGithubToken,
                            onValueChange = { newGithubToken = it },
                            label = { Text(if (isViet) "GitHub Token (Tùy chọn)" else "GitHub Token (Optional)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF388BFD),
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = themeColors.text,
                                unfocusedTextColor = themeColors.text
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EA043)),
                        onClick = {
                            if (newProjName.isNotEmpty()) {
                                viewModel.createProject(newProjName, newProjDesc, newGithubUser, newGithubToken)
                                newProjName = ""
                                newProjDesc = ""
                                newGithubUser = ""
                                newGithubToken = ""
                                showAddProjectDialog = false
                            }
                        }
                    ) {
                        Text(if (isViet) "Tạo mới" else "Create", color = themeColors.text)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddProjectDialog = false }) {
                        Text(if (isViet) "Hủy" else "Cancel", color = Color.LightGray)
                    }
                },
                containerColor = themeColors.background
            )
        }

        if (showAddFileDialog) {
            AlertDialog(
                onDismissRequest = { showAddFileDialog = false },
                title = { 
                    Text(
                        text = if (isViet) {
                            if (selectedTargetPath.isEmpty()) "Tạo tệp mới ở thư mục gốc" else "Tạo tệp mới trong /${selectedTargetPath}"
                        } else {
                            if (selectedTargetPath.isEmpty()) "Create new file at root" else "Create new file in /${selectedTargetPath}"
                        }, 
                        color = themeColors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                text = {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text(if (isViet) "Tên tệp (Ví dụ: script.js, index.html)" else "File name (E.g. script.js, index.html)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF388BFD),
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = themeColors.text,
                            unfocusedTextColor = themeColors.text
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EA043)),
                        onClick = {
                            if (newFileName.isNotEmpty()) {
                                viewModel.addNewFileOrFolder(newFileName, selectedTargetPath, isFolder = false)
                                newFileName = ""
                                showAddFileDialog = false
                            }
                        }
                    ) {
                        Text(if (isViet) "Tạo tệp" else "Create file", color = themeColors.text)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddFileDialog = false }) {
                        Text(if (isViet) "Hủy" else "Cancel", color = Color.LightGray)
                    }
                },
                containerColor = themeColors.background
            )
        }

        if (showAddFolderDialog) {
            AlertDialog(
                onDismissRequest = { showAddFolderDialog = false },
                title = { 
                    Text(
                        text = if (isViet) {
                            if (selectedTargetPath.isEmpty()) "Tạo thư mục mới ở gốc" else "Tạo thư mục mới trong /${selectedTargetPath}"
                        } else {
                            if (selectedTargetPath.isEmpty()) "Create new folder at root" else "Create new folder in /${selectedTargetPath}"
                        }, 
                        color = themeColors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text(if (isViet) "Tên thư mục mới (Ví dụ: assets, styles)" else "New folder name (E.g. assets, styles)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF388BFD),
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = themeColors.text,
                            unfocusedTextColor = themeColors.text
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EA043)),
                        onClick = {
                            if (newFolderName.isNotEmpty()) {
                                viewModel.addNewFileOrFolder(newFolderName, selectedTargetPath, isFolder = true)
                                // Auto-expand parent path
                                if (selectedTargetPath.isNotEmpty()) {
                                    expandedState[selectedTargetPath] = true
                                }
                                expandedState[if (selectedTargetPath.isEmpty()) newFolderName else "$selectedTargetPath/$newFolderName"] = true
                                newFolderName = ""
                                showAddFolderDialog = false
                            }
                        }
                    ) {
                        Text(if (isViet) "Tạo thư mục" else "Create folder", color = themeColors.text)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddFolderDialog = false }) {
                        Text(if (isViet) "Hủy" else "Cancel", color = Color.LightGray)
                    }
                },
                containerColor = themeColors.background
            )
        }

        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text(if (isViet) "Đổi tên tệp" else "Rename file", color = themeColors.text) },
                text = {
                    OutlinedTextField(
                        value = renameInputName,
                        onValueChange = { renameInputName = it },
                        label = { Text(if (isViet) "Nhập tên mới" else "Enter new name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF388BFD),
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = themeColors.text,
                            unfocusedTextColor = themeColors.text
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EA043)),
                        onClick = {
                            val target = fileToRename
                            if (target != null && renameInputName.isNotEmpty()) {
                                viewModel.renameFile(target, renameInputName)
                            }
                            showRenameDialog = false
                            fileToRename = null
                        }
                    ) {
                        Text(if (isViet) "Thay đổi" else "Change", color = themeColors.text)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showRenameDialog = false 
                        fileToRename = null
                    }) {
                        Text(if (isViet) "Hủy" else "Cancel", color = Color.LightGray)
                    }
                },
                containerColor = themeColors.background
            )
        }
    }
}
