package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.SidebarTab
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: EditorViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF090D14)
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    
                    if (showSplash) {
                        SplashLoadingScreen(
                            onFinished = { showSplash = false },
                            onLanguageSelected = { isViet ->
                                viewModel.toggleLanguage(isViet)
                            }
                        )
                    } else {
                        WorkspaceScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun SplashLoadingScreen(onFinished: () -> Unit, onLanguageSelected: (Boolean) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("monaco_prefs", android.content.Context.MODE_PRIVATE) }
    val initialLanguageSelected = remember { prefs.contains("is_vietnamese") }

    var languageSelected by remember { mutableStateOf(initialLanguageSelected) }

    if (!languageSelected) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B11)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Select Language / Chọn ngôn ngữ",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            onLanguageSelected(false)
                            languageSelected = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58A6FF))
                    ) {
                        Text("English",color=Color.White, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = {
                            onLanguageSelected(true)
                            languageSelected = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58A6FF))
                    ) {
                        Text("Tiếng Việt",color=Color.White, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    } else {
        val isSplashViet = remember(languageSelected) { prefs.getBoolean("is_vietnamese", false) }
    
        val progressVal = remember { androidx.compose.animation.core.Animatable(0f) }
        LaunchedEffect(Unit) {
            progressVal.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 2300,
                    easing = LinearEasing
                )
            )
            onFinished()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF060912)),
            contentAlignment = Alignment.Center
        ) {
            // Elegant background grid and radial ambient light glows
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepX = 40.dp.toPx()
                val stepY = 40.dp.toPx()
                val width = size.width
                val height = size.height
                
                // Draw sophisticated tech grid lines
                var x = 0f
                while (x < width) {
                    drawLine(
                        color = Color(0xFF58A6FF).copy(alpha = 0.05f),
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, height),
                        strokeWidth = 1f
                    )
                    x += stepX
                }
                var y = 0f
                while (y < height) {
                    drawLine(
                        color = Color(0xFF58A6FF).copy(alpha = 0.05f),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(width, y),
                        strokeWidth = 1f
                    )
                    y += stepY
                }
                
                // Radiant glow element focused in the center
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF58A6FF).copy(alpha = 0.16f),
                            Color(0xFFBC58FF).copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(width / 2, height / 2),
                        radius = size.minDimension / 1.1f
                    )
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Glowing Code Bracket Glowing Icon with subtle pulse animation
                val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.96f,
                    targetValue = 1.04f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .size(104.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF0F172A))
                        .border(
                            BorderStroke(
                                2.dp, 
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF58A6FF),
                                        Color(0xFFBC58FF)
                                    )
                                )
                            ), 
                            RoundedCornerShape(28.dp)
                        )
                ) {
                    // Inside radial glow backlighting
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF58A6FF).copy(alpha = 0.2f), Color.Transparent)
                                )
                            )
                    )
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = Color(0xFF58A6FF),
                        modifier = Modifier.size(54.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "CODE STUDIO",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    letterSpacing = 4.sp
                )
                
                Text(
                    text = "PROFESSIONAL WORKSPACE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF58A6FF),
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 36.dp)
                )

                // Elegant Loading Progress Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0E131F)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = progressVal.value,
                            color = Color(0xFF58A6FF),
                            trackColor = Color(0xFF161B22),
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                Text(
                    text = if (isSplashViet) "Tác giả: Phúc Nguyễn" else "Author: Phúc Nguyễn",
                    color = Color.Gray.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@kotlin.OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun WorkspaceScreen(viewModel: EditorViewModel = viewModel()) {
    if (viewModel.isFirstLaunch) {
        WelcomeScreen(viewModel)
        return
    }
    
    if (viewModel.hasAppLock && !viewModel.isAppUnlocked) {
        LockScreen(viewModel)
        return
    }

    var isPanelExpanded by remember { mutableStateOf(false) }
    
    val isViet = viewModel.isVietnamese
    val themeColors = ThemeRegistry.getTheme(viewModel.editorTheme)
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Automatically close the Workspace tree drawer when the user taps on a file, for maximum screen layout flow
    LaunchedEffect(viewModel.activeFile) {
        scope.launch {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = themeColors.background,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(themeColors.background)
                        .padding(16.dp)
                ) {
                    // Header inside drawer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = themeColors.keyword,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isViet) "KHÔNG GIAN LÀM VIỆC" else "PROJECT WORKSPACE",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Divider(color = Color.DarkGray.copy(alpha = 0.4f), modifier = Modifier.padding(bottom = 12.dp))
                    
                    // Render FileTree directly inside Left Drawer Sidebar comfort
                    Box(modifier = Modifier.weight(1f)) {
                        FileTree(viewModel = viewModel)
                    }
                    
                    // Close button inside drawer footer
                    Button(
                        onClick = { scope.launch { drawerState.close() } },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.headerBackground),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(if (isViet) "Đóng" else "Close Sidebar", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                if (!WindowInsets.isImeVisible) {
                    // High-End Scrollable Navigation bar to control sidebar categories on tiny and fold screens alike
                    Surface(
                        color = themeColors.headerBackground,
                        tonalElevation = 8.dp,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        var showMoreMenu by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val mainTabs = listOf(
                                Triple(null, Icons.Default.Code, "Code"),
                                Triple(SidebarTab.PREVIEW, Icons.Default.PlayArrow, "Run"),
                                Triple(SidebarTab.GITHUB, Icons.Default.CloudQueue, "GitHub"),
                                Triple(SidebarTab.GEMINI, Icons.Default.AutoAwesome, if (isViet) "Trợ lý AI" else "AI Asst")
                            )
                            val moreTabs = listOf(
                                Triple(SidebarTab.SNIPPETS, Icons.Default.Bookmarks, "Snippet"),
                                Triple(SidebarTab.OUTLINE, Icons.Default.AccountTree, "Outline"),
                                Triple(SidebarTab.DIFF, Icons.Default.Compare, "Diff"),
                                Triple(SidebarTab.SETTINGS, Icons.Default.Settings, if (isViet) "Cài đặt" else "Settings")
                            )

                            mainTabs.forEach { (tab, icon, label) ->
                                val isActive = if (tab == null) !isPanelExpanded else (isPanelExpanded && viewModel.activeTab == tab)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isActive) themeColors.background else Color.Transparent)
                                        .clickable {
                                            if (tab == null) {
                                                isPanelExpanded = false
                                            } else {
                                                viewModel.activeTab = tab
                                                isPanelExpanded = true
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isActive) themeColors.keyword else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActive) themeColors.keyword else themeColors.text.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Box {
                                val isMoreActive = isPanelExpanded && moreTabs.any { it.first == viewModel.activeTab }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isMoreActive) themeColors.background else Color.Transparent)
                                        .clickable { showMoreMenu = true }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreHoriz,
                                        contentDescription = "More",
                                        tint = if (isMoreActive) themeColors.keyword else themeColors.text.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isViet) "Thêm" else "More",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isMoreActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isMoreActive) themeColors.keyword else themeColors.text.copy(alpha = 0.5f)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                    modifier = Modifier.background(themeColors.headerBackground)
                                ) {
                                    moreTabs.forEach { (tab, icon, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label, color = themeColors.text, fontSize = 12.sp) },
                                            leadingIcon = { Icon(icon, contentDescription = null, tint = themeColors.keyword, modifier = Modifier.size(16.dp)) },
                                            onClick = {
                                                if (tab != null) {
                                                    viewModel.activeTab = tab
                                                    isPanelExpanded = true
                                                }
                                                showMoreMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(themeColors.background)
                    .padding(innerPadding)
            ) {
                // High-End Monaco Header with Workspace Drawer sliding triggers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColors.headerBackground)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 1. Tông màu Menu toggling Left Workspace Drawer
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Mở Cây thư mục",
                                tint = themeColors.keyword,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(6.dp))
                        
                        Column {
                            Text(
                                text = "CODE STUDIO",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.keyword,
                                letterSpacing = 1.0.sp
                            )
                            Text(
                                text = viewModel.currentProject?.name ?: (if (isViet) "Chưa mở dự án" else "Empty Project"),
                                fontSize = 11.sp,
                                color = themeColors.text.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick Run Preview trigger
                        IconButton(
                            onClick = {
                                viewModel.activeTab = SidebarTab.PREVIEW
                                isPanelExpanded = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Html,
                                contentDescription = "Xem Live HTML",
                                tint = Color(0xFFE06C75),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Switch split or collapse panel
                        IconButton(
                            onClick = { isPanelExpanded = !isPanelExpanded },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isPanelExpanded) Icons.Default.Code else Icons.Default.Terminal,
                                contentDescription = "Toggle Mode",
                                tint = themeColors.text.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Adaptive screen layout based on toggle. When a panel is expanded, it takes up FULL SCREEN.
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (!isPanelExpanded) {
                        // FULL-SCREEN EDITOR VIEW
                        MonacoEditor(viewModel = viewModel)
                    } else {
                        // FULL-SCREEN ACTIVE WORKSPACE PANEL VIEW (No split Screen crowding!)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(themeColors.background)
                        ) {
                            when (viewModel.activeTab) {
                                SidebarTab.FILES -> {
                                    FileTree(viewModel = viewModel)
                                }
                                SidebarTab.GITHUB -> GitHubPanel(viewModel = viewModel)
                                SidebarTab.GEMINI -> GeminiPanel(viewModel = viewModel)
                                SidebarTab.PREVIEW -> WebPreview(viewModel = viewModel)
                                SidebarTab.SNIPPETS -> SnippetPanel(viewModel = viewModel)
                                SidebarTab.OUTLINE -> OutlinePanel(viewModel = viewModel)
                                SidebarTab.DIFF -> DiffPanel(viewModel = viewModel)
                                SidebarTab.SETTINGS -> SettingsPanel(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
