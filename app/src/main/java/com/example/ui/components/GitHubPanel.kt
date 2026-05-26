package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.viewmodel.EditorViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@Composable
fun GitHubPanel(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val themeColors = ThemeRegistry.getTheme(viewModel.editorTheme)
    val isViet = viewModel.isVietnamese
    val searchRepos by viewModel.githubRepos.collectAsState()
    val exploredItems by viewModel.exploredItems.collectAsState()
    val currentRepo = viewModel.exploredRepo
    val isSearching = viewModel.isSearchingGithub
    val isLoadingContents = viewModel.isLoadingExploredItems
    val errorString = viewModel.githubQueryError

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .padding(12.dp)
    ) {
        if (currentRepo == null) {
            var showCreateRepoDialog by remember { mutableStateOf(false) }

            // Searh state layout
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isViet) "TÌM KIẾM GITHUB" else "GITHUB SEARCH",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                )
                if (viewModel.githubToken.isNotEmpty()) {
                    Text(
                        text = if (isViet) "+ Tạo Mới" else "+ New Repo",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF58A6FF),
                        modifier = Modifier.clickable { showCreateRepoDialog = true }
                    )
                }
            }
            
            if (showCreateRepoDialog) {
                var repoName by remember { mutableStateOf("") }
                var repoDesc by remember { mutableStateOf("") }
                var isPrivate by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showCreateRepoDialog = false },
                    title = { Text(if (isViet) "Tạo Kho Lưu Trữ Mới" else "Create New Repository", color = themeColors.text) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = repoName,
                                onValueChange = { repoName = it },
                                label = { Text("Repository Name") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = repoName.isEmpty()
                            )
                            OutlinedTextField(
                                value = repoDesc,
                                onValueChange = { repoDesc = it },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Private Repository", color = themeColors.text)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { 
                                viewModel.createGithubRepo(repoName, repoDesc, isPrivate)
                                showCreateRepoDialog = false
                            },
                            enabled = repoName.isNotEmpty() && !viewModel.isCreatingRepo
                        ) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateRepoDialog = false }) {
                            Text("Cancel")
                        }
                    },
                    containerColor = themeColors.background,
                    titleContentColor = themeColors.text,
                    textContentColor = themeColors.text
                )
            }

            // High aesthetic Search Field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeColors.headerBackground)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )

                BasicTextField(
                    value = viewModel.githubQuery,
                    onValueChange = { viewModel.githubQuery = it },
                    textStyle = LocalTextStyle.current.copy(color = themeColors.text, fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.searchGithub() }),
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    decorationBox = { innerTextField ->
                        if (viewModel.githubQuery.isEmpty()) {
                            Text(text = if (isViet) if (isViet) "Nhập tên kho lưu trữ hoặc từ khóa..." else "Search repositories or keywords..." else "Search repositories or keywords...", color = Color.Gray, fontSize = 13.sp)
                        }
                        innerTextField()
                    }
                )

                if (viewModel.githubQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.githubQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isSearching) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF388BFD))
                }
            } else if (errorString != null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = errorString, color = Color(0xFFF85149), style = MaterialTheme.typography.bodySmall)
                }
            } else if (searchRepos.isEmpty()) {
                val trendingList by viewModel.trendingRepos.collectAsState()
                
                LaunchedEffect(Unit) {
                    if (trendingList.isEmpty() && !viewModel.isLoadingTrending) {
                        viewModel.fetchTrendingRepos("kotlin") // Fetch trending kotlin repos as default
                    }
                }

                if (viewModel.isLoadingTrending) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF388BFD))
                    }
                } else {
                    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "🔥 Trending Repositories", 
                                color = themeColors.text, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 14.sp
                            )
                            if (viewModel.githubUsername.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        viewModel.githubQuery = "user:${viewModel.githubUsername}"
                                        viewModel.searchGithub()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F52BA)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("My Repos", fontSize = 10.sp)
                                }
                            }
                        }
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(trendingList) { repo ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { viewModel.exploreGithubRepo(repo) },
                                    colors = CardDefaults.cardColors(containerColor = themeColors.headerBackground)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = repo.owner.avatarUrl,
                                            contentDescription = "Avatar",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(repo.fullName, style = MaterialTheme.typography.bodyMedium, color = themeColors.text, fontWeight = FontWeight.Bold)
                                            if (!repo.description.isNullOrEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(repo.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 2)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${repo.stars}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(searchRepos) { repo ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.exploreGithubRepo(repo) },
                            colors = CardDefaults.cardColors(containerColor = themeColors.headerBackground)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = repo.owner.avatarUrl,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = repo.fullName,
                                        color = Color(0xFF58A6FF),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1
                                    )
                                    if (repo.description != null) {
                                        Text(
                                            text = repo.description,
                                            color = Color.LightGray,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.StarBorder,
                                        contentDescription = "Stars",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = repo.stars.toString(),
                                        color = themeColors.text,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Inside chosen repo contents explore mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.goBackExploredRepo() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isViet) "Quay lại" else "Back",
                        tint = themeColors.text
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentRepo.name,
                        color = themeColors.text,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Path: /" + viewModel.exploredRepoPath,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }

            var activeSubTab by remember { mutableStateOf("dashboard") }

            val tabs = listOf(
                "dashboard" to (if (isViet) "Tổng quan" else "Dashboard"),
                "code" to (if (isViet) "Mã nguồn" else "Code"),
                "commits" to (if (isViet) "Lịch sử Commits" else "Commits"),
                "graph" to (if (isViet) "Cây Git" else "Git Graph"),
                "pr" to (if (isViet) "Yêu cầu kéo (PR)" else "Pull Requests"),
                "publish" to (if (isViet) "Đẩy mã (Push)" else "Push")
            )

            ScrollableTabRow(
                selectedTabIndex = tabs.indexOfFirst { it.first == activeSubTab }.coerceAtLeast(0),
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    val index = tabs.indexOfFirst { it.first == activeSubTab }.coerceAtLeast(0)
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[index]),
                        color = Color(0xFF388BFD)
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                tabs.forEach { (id, label) ->
                    Tab(
                        selected = activeSubTab == id,
                        onClick = { activeSubTab = id },
                        text = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (activeSubTab == id) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeSubTab == id) Color(0xFF388BFD) else Color.Gray
                            )
                        }
                    )
                }
            }

            if (activeSubTab == "dashboard") {
                val detail = viewModel.currentRepoDetail
                if (viewModel.isLoadingRepoDetail) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF388BFD))
                    }
                } else if (detail != null) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (isViet) "THÔNG TIN DỰ ÁN" else "PROJECT INFO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.text
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                                Text("${detail.stars}", fontWeight = FontWeight.Bold, color = themeColors.text)
                                Text("Stars", fontSize = 10.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CallSplit, contentDescription = null, tint = Color(0xFF2EA043))
                                Text("${detail.forks}", fontWeight = FontWeight.Bold, color = themeColors.text)
                                Text("Forks", fontSize = 10.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.BugReport, contentDescription = null, tint = Color(0xFFF85149))
                                Text("${detail.openIssues}", fontWeight = FontWeight.Bold, color = themeColors.text)
                                Text("Issues", fontSize = 10.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF58A6FF))
                                Text("${detail.watchers}", fontWeight = FontWeight.Bold, color = themeColors.text)
                                Text("Watchers", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        if (!detail.description.isNullOrEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.headerBackground),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = detail.description,
                                    modifier = Modifier.padding(12.dp),
                                    color = themeColors.text,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (viewModel.githubToken.isNotEmpty()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.starExploredRepo() },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    enabled = !viewModel.isStarringRepo,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F52BA)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (viewModel.isStarringRepo) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Star, contentDescription = "Star", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isViet) "Star" else "Star", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                Button(
                                    onClick = { viewModel.forkExploredRepo() },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    enabled = !viewModel.isForkingRepo,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F52BA)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (viewModel.isForkingRepo) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.CallSplit, contentDescription = "Fork", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isViet) "Fork" else "Fork", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.cloneRepoToWorkspace(currentRepo) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            enabled = !viewModel.isCloningRepo,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2EA043),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF2EA043).copy(alpha = 0.5f),
                                disabledContentColor = Color.White.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (viewModel.isCloningRepo) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isViet) "Đang Clone..." else "Cloning...")
                            } else {
                                Icon(Icons.Default.Download, contentDescription = "Clone")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isViet) "Clone vào Workspace" else "Clone into Workspace", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                     Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(if (isViet) "Không thể tải dữ liệu." else "Failed to load data.", color = Color.Gray)
                    }
                }
            } else if (activeSubTab == "commits") {
                val commits by viewModel.githubCommits.collectAsState()
                if (viewModel.isLoadingCommits) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF388BFD))
                    }
                } else if (commits.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = if (isViet) "Không có dữ liệu lịch sử commit." else "No commit history found.", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(commits) { commitItem ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = themeColors.headerBackground),
                                border = BorderStroke(1.dp, Color.DarkGray.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    val avatarUrl = commitItem.authorInfo?.avatarUrl
                                    if (avatarUrl != null) {
                                        AsyncImage(
                                            model = avatarUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(13.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(13.dp))
                                                .background(Color.DarkGray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Color.LightGray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = commitItem.commitDetail.message,
                                            color = themeColors.text,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = commitItem.commitDetail.author.name,
                                                color = Color(0xFFFFD700),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = if (isViet) "đã commit " else "committed " + commitItem.commitDetail.author.date.take(10),
                                                color = Color.Gray,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = commitItem.sha.take(7),
                                            color = Color(0xFF58A6FF),
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (activeSubTab == "pr") {
                val prs by viewModel.githubPullRequests.collectAsState()
                if (viewModel.isLoadingPullRequests) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF388BFD))
                    }
                } else if (prs.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = if (isViet) "Không có Pull Requests nào." else "No Pull Requests found.", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(prs) { pr ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = themeColors.headerBackground),
                                border = BorderStroke(1.dp, Color.DarkGray.copy(alpha = 0.2f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.CallMerge, 
                                        contentDescription = null, 
                                        tint = if (pr.state == "open") Color(0xFF2EA043) else Color(0xFF8957E5),
                                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = pr.title,
                                            color = themeColors.text,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "#${pr.number} opened by ${pr.user.login}",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (activeSubTab == "graph") {
                val commits by viewModel.githubCommits.collectAsState()
                if (viewModel.isLoadingCommits) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF388BFD))
                    }
                } else if (commits.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = if (isViet) "Không có dữ liệu graph." else "No graph data found.", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(commits) { commit ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.width(20.dp).height(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Spacer(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color(0xFF388BFD).copy(alpha=0.5f)))
                                    Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFF388BFD)))
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha=0.3f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = commit.sha.take(7),
                                        color = Color(0xFF58A6FF),
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = commit.commitDetail.message,
                                    color = themeColors.text,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } else if (activeSubTab == "publish") {
                // VS Code source control publish manager
                val localFiles by viewModel.projectFiles.collectAsState()
                val context = androidx.compose.ui.platform.LocalContext.current
                var targetBranch by remember { mutableStateOf("main") }
                var commitMsg by remember { mutableStateOf("Update workspace via Code Studio") }
                var isGeneratingMsg by remember { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = if (isViet) "TRÌNH KIỂM SOÁT PHIÊN BẢN (VCS)" else "VERSION CONTROL SYSTEM (VCS)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF388BFD)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isViet) "Chọn tệp từ kho lưu trữ cục bộ để đồng bộ với kho lưu trữ từ xa trên GitHub." else "Select a local workspace file to push to the remote GitHub repository.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            lineHeight = 16.sp
                        )
                    }

                    if (viewModel.githubToken.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF302217)),
                                border = BorderStroke(1.dp, Color(0xFFD17A19).copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD17A19), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isViet) "Chưa cấu hình Access Token" else "Access Token Not Configured", fontWeight = FontWeight.Bold, color = Color(0xFFE3B341), fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isViet) "Vui lòng cung cấp Personal Access Token trong phần Cài đặt để xác thực quyền ghi đối với kho lưu trữ từ xa." else "Please configure your Personal Access Token in Settings to authorize write access to remote repositories.",
                                        fontSize = 11.sp,
                                        color = Color.LightGray,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = if (isViet) "1. ĐỒNG BỘ TOÀN BỘ WORKSPACE" else "1. SYNC ENTIRE WORKSPACE",
                            style = MaterialTheme.typography.labelSmall,
                            color = themeColors.text,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isViet) "Tất cả các tệp trong không gian làm việc sẽ được gộp chung thành một commit để đảm bảo tính đồng nhất (tránh rác commit)." 
                                 else "All files in your local workspace will be grouped into a single commit to maintain clean branch history.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    item {
                        Text(
                            text = if (isViet) "2. CẤU HÌNH COMMIT & REMOTE" else "2. COMMIT & REMOTE CONFIGURATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = themeColors.text,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Target branch name
                            OutlinedTextField(
                                value = targetBranch,
                                onValueChange = { targetBranch = it },
                                label = { Text(if (isViet) "Nhánh đích (Target Branch)" else "Target Branch") },
                                placeholder = { Text(if (isViet) "VD: main, master" else "e.g., main, master") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF388BFD),
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = themeColors.text,
                                    unfocusedTextColor = themeColors.text,
                                    focusedLabelColor = Color(0xFF388BFD),
                                    unfocusedLabelColor = Color.Gray
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 13.sp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Commit message integration with Gemini
                            val scope = rememberCoroutineScope()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isViet) "Nội dung Commit (Message)" else "Commit Message",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = themeColors.text
                                )
                                TextButton(onClick = {
                                    if (viewModel.geminiApiKey.isEmpty()) {
                                        android.widget.Toast.makeText(context, if (isViet) "Vui lòng nhập Gemini API Key trong Cài Đặt!" else "Set Gemini API Key in Settings first!", android.widget.Toast.LENGTH_SHORT).show()
                                        return@TextButton
                                    }
                                    isGeneratingMsg = true
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        try {
                                            val fileSummaries = localFiles.filter { !it.isFolder }.joinToString("\n") { "File info: ${it.path}\nContent Snippet: ${it.content.take(150)}" }
                                            val prompt = "Create a short, professional git commit message (just the message, no quotes) for the following workspace files changes:\n$fileSummaries"
                                            val msg = com.example.api.GeminiRetrofitClient.generateContent(
                                                prompt = prompt,
                                                model = viewModel.activeAiModel,
                                                customApiKey = viewModel.geminiApiKey
                                            )
                                            commitMsg = msg
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Gemini Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isGeneratingMsg = false
                                        }
                                    }
                                }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) {
                                    if (isGeneratingMsg) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color(0xFFFFD700), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isViet) "AI Tạo (Auto)" else "Auto Gen", fontSize = 11.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = commitMsg,
                                onValueChange = { commitMsg = it },
                                placeholder = { Text(if (isViet) "Miêu tả thay đổi..." else "Describe your changes...") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF388BFD),
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = themeColors.text,
                                    unfocusedTextColor = themeColors.text
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 13.sp)
                            )
                        }

                        // Success / Error notification statuses direct link
                        viewModel.githubPushSuccess?.let { successText ->
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF142C18)),
                                    border = BorderStroke(1.dp, Color(0xFF2EA043).copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2EA043), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(text = successText, color = Color(0xFF2EA043), fontSize = 11.sp, lineHeight = 14.sp)
                                    }
                                }
                            }
                        }

                        viewModel.githubPushError?.let { errText ->
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1616)),
                                    border = BorderStroke(1.dp, Color(0xFFF85149).copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFF85149), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(text = errText, color = Color(0xFFF85149), fontSize = 11.sp, lineHeight = 14.sp)
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            if (viewModel.isPushingToGithub) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF2EA043), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(if (isViet) "Đang thực thi lệnh đẩy lên remote..." else "Pushing to remote repository...", color = themeColors.text, fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val o = currentRepo?.owner?.login ?: "unknown"
                                        val r = currentRepo?.name ?: "unknown"
                                        viewModel.commitAndPushAllWorkspaceFiles(
                                            owner = o,
                                            repo = r,
                                            commitMessage = commitMsg,
                                            branch = targetBranch
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EA043)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isViet) "ĐẨY LÊN KHO LƯU TRỮ TỪ XA" else "PUSH TO REMOTE REPOSITORY",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = themeColors.text
                                    )
                                }
                            }
                        }
                    }
                } else {
                val expandedState = remember { mutableStateMapOf<String, Boolean>() }
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    item {
                        val o = currentRepo?.owner?.login ?: "unknown"
                        val r = currentRepo?.name ?: "unknown"
                        GithubRecursiveTree(
                            owner = o,
                            repoName = r,
                            path = "",
                            depth = 0,
                            viewModel = viewModel,
                            expandedState = expandedState
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GithubRecursiveTree(
    owner: String,
    repoName: String,
    path: String,
    depth: Int,
    viewModel: EditorViewModel,
    expandedState: MutableMap<String, Boolean>
) {
    val treeCache by viewModel.githubTreeCache.collectAsState()
    val children = treeCache[path]
    val isViet = viewModel.isVietnamese

    if (children == null) {
        androidx.compose.runtime.LaunchedEffect(path) {
            viewModel.fetchGithubFolderItems(path)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = (depth * 14).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF388BFD), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isViet) "Đang tải..." else "Loading...", color = Color.Gray, fontSize = 12.sp)
        }
    } else {
        Column {
            children.forEach { child ->
                val isDir = child.type == "dir"
                val isExpanded = expandedState[child.path] ?: false

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            if (isDir) {
                                expandedState[child.path] = !isExpanded
                            } else {
                                viewModel.importGithubFile(child)
                            }
                        }
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(8.dp + (depth * 14).dp))

                        if (isDir) {
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

                        Icon(
                            imageVector = if (isDir) {
                                if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder
                            } else {
                                Icons.Default.InsertDriveFile
                            },
                            contentDescription = null,
                            tint = if (isDir) Color(0xFFE3B341) else Color(0xFF58A6FF),
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = child.name,
                            color = Color(0xFFC9D1D9),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (!isDir) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = if (isViet) "Tải về" else "Download",
                            tint = Color(0xFF2EA043),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isDir && isExpanded) {
                    GithubRecursiveTree(
                        owner = owner,
                        repoName = repoName,
                        path = child.path,
                        depth = depth + 1,
                        viewModel = viewModel,
                        expandedState = expandedState
                    )
                }
            }
        }
    }
}

