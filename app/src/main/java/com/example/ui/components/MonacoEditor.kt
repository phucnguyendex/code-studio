package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.database.FileEntity
import com.example.viewmodel.EditorViewModel
import java.util.regex.Pattern

// Syntax Diagnostics structure to resemble VSCode error line highlightings
data class SyntaxProblem(
    val line: Int,
    val message: String,
    val severity: String = "Error" // "Error" or "Warning"
)

// Document signature and detailed notes mapping for autocomplete items
data class KeywordDoc(val signature: String, val description: String)

fun getDocForKeyword(word: String, lang: String): KeywordDoc {
    val lowerWord = word.lowercase()
    val lowerLang = lang.lowercase()
    return when {
        lowerLang == "kotlin" -> {
            when (word) {
                "fun" -> KeywordDoc("fun name(args): Type", "Khai báo hàm xử lý hoặc thành viên lớp.")
                "val" -> KeywordDoc("val name = value", "Khai báo hằng số bất biến (chỉ đọc, read-only value).")
                "var" -> KeywordDoc("var name = value", "Khai báo biến số động (có thể gán lại giá trị).")
                "class" -> KeywordDoc("class Name", "Khai báo lớp (Class) để khởi tạo các đối tượng hướng cấu trúc.")
                "interface" -> KeywordDoc("interface Name", "Định nghĩa hợp đồng giao diện cấu trúc cho các lớp khác hiện thực.")
                "object" -> KeywordDoc("object Singleton", "Định nghĩa một phần tử đơn nhất (Singleton object).")
                "println()" -> KeywordDoc("println(message)", "In thông điệp ra dòng ra chuẩn kèm theo dòng mới.")
                "Modifier" -> KeywordDoc("Modifier", "Đối tượng cấu hình cốt lõi dùng để định dạng, padding, click và styles cho Compose UI.")
                "Modifier.fillMaxWidth()" -> KeywordDoc("Modifier.fillMaxWidth(fraction)", "Cấu hình mở rộng lấp đầy chiều rộng tối đa hiện có của vùng chứa.")
                "Modifier.fillMaxSize()" -> KeywordDoc("Modifier.fillMaxSize()", "Mở rộng component lấp đầy tối đa cả chiều rộng và chiều cao.")
                "Modifier.padding()" -> KeywordDoc("Modifier.padding(dp)", "Thêm khoảng trống đệm bao xung quanh nội dung component.")
                "Modifier.background()" -> KeywordDoc("Modifier.background(color, shape)", "Vẽ màu nền hoặc hiệu ứng chuyển sắc cho nền component.")
                "Column" -> KeywordDoc("Column(modifier, verticalArrangement...) {\n    \n}", "Bố cục sắp xếp liên tiếp các phần tử theo chiều dọc.")
                "Row" -> KeywordDoc("Row(modifier, horizontalArrangement...) {\n    \n}", "Bố cục sắp xếp các phần tử nằm ngang kế tiếp nhau.")
                "Box" -> KeywordDoc("Box(modifier, contentAlignment...) {\n    \n}", "Bố cục xếp chồng lớp phần tử tự do (Z-layering).")
                "Text" -> KeywordDoc("Text(text: String, color: Color, ...)", "Phần tử hiển thị chuỗi ký tự văn bản tùy biến kiểu chữ.")
                "Button" -> KeywordDoc("Button(onClick: () -> Unit) {\n    \n}", "Nút nhấn tương tác kích hoạt tiến trình xử lý logic.")
                "remember { mutableStateOf() }" -> KeywordDoc("remember { mutableStateOf(initial) }", "Ghi nhớ trạng thái thay đổi để tự động kích hoạt render làm mới UI.")
                "viewModelScope.launch" -> KeywordDoc("viewModelScope.launch { ... }", "Khởi chạy một tiểu trình (coroutine) gán liền với vòng đời ViewModel.")
                "collectAsStateWithLifecycle()" -> KeywordDoc("collectAsStateWithLifecycle()", "Thu thập luồng dữ liệu an toàn tối ưu theo vòng đời thiết bị.")
                else -> KeywordDoc(word, "Từ khóa/Hàm hỗ trợ viết mã nguồn trong ngôn ngữ Kotlin.")
            }
        }
        lowerLang == "javascript" || lowerLang == "typescript" -> {
            when (word) {
                "const" -> KeywordDoc("const name = value", "Khai báo hằng số bảo vệ phạm vi khối cục bộ.")
                "let" -> KeywordDoc("let name = value", "Khai báo biến số thay đổi phạm vi khối cục bộ.")
                "var" -> KeywordDoc("var name = value", "Khai báo biến số phạm vi hàm cổ điển.")
                "function" -> KeywordDoc("function name(params) {\n    \n}", "Khai báo một hàm thực thi có thể gọi từ nơi khác.")
                "console.log()" -> KeywordDoc("console.log(...args)", "In thông điệp chẩn đoán chi tiết ra bảng điều khiển Dev Console.")
                "document.getElementById()" -> KeywordDoc("document.getElementById(id)", "Truy tìm phần tử DOM đầu tiên sở hữu ID tương ứng.")
                "addEventListener" -> KeywordDoc("target.addEventListener(event, callback)", "Lắng nghe sự kiện tương tác chuột/bàn phím tương ứng.")
                "fetch" -> KeywordDoc("fetch(url, options): Promise", "Gửi yêu cầu HTTP nạp hoặc cập nhật tài nguyên từ API.")
                "async" -> KeywordDoc("async function()", "Đánh dấu hàm hoạt động bất đồng bộ luôn trả về một Promise.")
                "await" -> KeywordDoc("await promise", "Chờ đợi Promise hoàn thành kết quả trước khi tiếp tục (Chỉ dùng trong async).")
                "localStorage" -> KeywordDoc("localStorage.setItem/getItem", "Bộ nhớ lưu trữ cục bộ persistent key-value phía browser.")
                "JSON.stringify()" -> KeywordDoc("JSON.stringify(object)", "Chuyển mã đối tượng JavaScript sang chuỗi JSON tiêu chuẩn.")
                "JSON.parse()" -> KeywordDoc("JSON.parse(string)", "Phân tích chuỗi JSON thô thành đối tượng JavaScript.")
                else -> KeywordDoc(word, "Từ khóa trong ngôn ngữ lập trình JavaScript / TypeScript.")
            }
        }
        lowerLang == "html" -> {
            when (word) {
                "div" -> KeywordDoc("<div>...</div>", "Khối chứa bố cục chung cách biệt (Block structural element).")
                "span" -> KeywordDoc("<span>...</span>", "Thẻ bọc các cụm từ nội dòng dòng chảy văn bản (Inline element).")
                "script" -> KeywordDoc("<script src=\"...\"></script>", "Nhúng hoặc thực thi mã kịch bản lập trình JavaScript dán tiếp.")
                "style" -> KeywordDoc("<style>...</style>", "Nhúng định dạng CSS trang hoàng giao diện trực diện.")
                "a" -> KeywordDoc("<a href=\"...\">Liên kết</a>", "Thẻ siêu liên kết điều hướng sang trang mạng khác.")
                "img" -> KeywordDoc("<img src=\"...\" alt=\"\" />", "Thẻ nhúng hiển thị hình ảnh đồ họa đa phương tiện.")
                "button" -> KeywordDoc("<button>Bấm</button>", "Phần tử nút bấm tương tác HTML tiêu chuẩn.")
                else -> KeywordDoc("<$word>", "Thẻ định nghĩa cấu trúc tài liệu HTML.")
            }
        }
        lowerLang == "css" -> {
            when (word) {
                "display: flex;" -> KeywordDoc("display: flex;", "Mở rộp cơ chế căn chỉnh Flexbox chuyển dịch giao diện linh động.")
                "justify-content: center;" -> KeywordDoc("justify-content: center;", "Căn lề các phần tử nằm giữa trung tâm trục chính Flex.")
                "align-items: center;" -> KeywordDoc("align-items: center;", "Căn lề các vị trí phần tử giữa theo trục dọc.")
                "background-color: " -> KeywordDoc("background-color: color;", "Tô màu sắc nền cho vùng chứa.")
                "border-radius: " -> KeywordDoc("border-radius: radius;", "Bo tròn các góc viền thô của thẻ.")
                else -> KeywordDoc(word, "Định dạng style trang trí của CSS.")
            }
        }
        lowerLang == "python" -> {
            when (word) {
                "def" -> KeywordDoc("def function_name(args):", "Khai báo định nghĩa hàm mới.")
                "class" -> KeywordDoc("class ClassName:", "Khai báo lớp biểu diễn đối tượng.")
                "print()" -> KeywordDoc("print(*objects, sep=' ', ...)", "Kết xuất dữ liệu ra thiết bị đầu ra chuẩn.")
                "len()" -> KeywordDoc("len(sequence)", "Trả về số lượng phần tử tập hợp hoặc độ dài chuỗi.")
                "import" -> KeywordDoc("import module_name", "Nhập thư viện ngoài vào môi trường viết.")
                "from" -> KeywordDoc("from module import ...", "Nhập các hàm xác định cụ thể trong mô-đun.")
                "self" -> KeywordDoc("self", "Biến tham chiếu đại diện cho đối tượng hiện tại của lớp.")
                else -> KeywordDoc(word, "Từ khóa trong ngôn ngữ kịch bản tối giản Python.")
            }
        }
        else -> KeywordDoc(word, "Tính năng hỗ trợ viết code thông minh.")
    }
}

// Highly optimized in-memory syntax problem checking
fun runSyntaxCheck(text: String, lang: String): List<SyntaxProblem> {
    val problems = mutableListOf<SyntaxProblem>()
    if (text.isEmpty()) return problems
    val lines = text.split("\n")
    val lowerLang = lang.lowercase()
    
    var openCurly = 0
    var openParentheses = 0
    var openSquare = 0
    
    val curlyHistory = mutableListOf<Int>()
    val parenHistory = mutableListOf<Int>()
    val squareHistory = mutableListOf<Int>()
    
    lines.forEachIndexed { index, line ->
        val lineNum = index + 1
        val cleanLine = line.replace(Regex("//.*|/\\*.*?\\*/|#.*"), "")
        
        // Match string balances
        val quoteCount = cleanLine.count { it == '"' }
        if (quoteCount % 2 != 0) {
            problems.add(SyntaxProblem(lineNum, "Chuỗi ký tự chưa đóng kín (thiếu dấu nháy kép `\"`)", "Warning"))
        }
        
        val singleQuoteCount = cleanLine.count { it == '\'' }
        if (singleQuoteCount % 2 != 0) {
            problems.add(SyntaxProblem(lineNum, "Chuỗi chưa đóng kín (thiếu dấu nháy đơn `'`)", "Warning"))
        }

        cleanLine.forEach { char ->
            when (char) {
                '{' -> { openCurly++; curlyHistory.add(lineNum) }
                '}' -> { 
                    openCurly--
                    if (openCurly < 0) {
                        problems.add(SyntaxProblem(lineNum, "Phát hiện dư dấu đóng ngoặc nhọn `}` thừa."))
                        openCurly = 0
                    } else if (curlyHistory.isNotEmpty()) {
                        curlyHistory.removeAt(curlyHistory.size - 1)
                    }
                }
                '(' -> { openParentheses++; parenHistory.add(lineNum) }
                ')' -> {
                    openParentheses--
                    if (openParentheses < 0) {
                        problems.add(SyntaxProblem(lineNum, "Phát hiện dư dấu đóng ngoặc đơn `)` thừa."))
                        openParentheses = 0
                    } else if (parenHistory.isNotEmpty()) {
                        parenHistory.removeAt(parenHistory.size - 1)
                    }
                }
                '[' -> { openSquare++; squareHistory.add(lineNum) }
                ']' -> {
                    openSquare--
                    if (openSquare < 0) {
                        problems.add(SyntaxProblem(lineNum, "Phát hiện dư dấu đóng ngoặc vuông `]` thừa."))
                        openSquare = 0
                    } else if (squareHistory.isNotEmpty()) {
                        squareHistory.removeAt(squareHistory.size - 1)
                    }
                }
            }
        }
        
        if (lowerLang == "kotlin" || lowerLang == "java" || lowerLang == "javascript" || lowerLang == "typescript") {
            if (cleanLine.trim().startsWith("fun ") && !cleanLine.contains("(") && !cleanLine.contains(")")) {
                problems.add(SyntaxProblem(lineNum, "Khai báo hàm hợp lệ phải bổ sung tham số `()`.", "Error"))
            }
            if (cleanLine.trim().startsWith("class ") && cleanLine.trim().length < 7) {
                problems.add(SyntaxProblem(lineNum, "Khai báo lớp trống rỗng thiếu định danh.", "Error"))
            }
        }
        
        if (lowerLang == "html") {
            val tagsOpen = cleanLine.count { it == '<' }
            val tagsClose = cleanLine.count { it == '>' }
            if (tagsOpen != tagsClose) {
                problems.add(SyntaxProblem(lineNum, "Thẻ HTML chưa được đóng định dạng đúng cách (thiếu `<` hoặc `>`)", "Warning"))
            }
        }
    }
    
    curlyHistory.forEach { lineNum ->
        problems.add(SyntaxProblem(lineNum, "Dấu ngoặc nhọn `{` chưa được khép kín bằng `}` hợp lệ.", "Error"))
    }
    parenHistory.forEach { lineNum ->
        problems.add(SyntaxProblem(lineNum, "Dấu ngoặc đơn `(` chưa được khép kín bằng `)` hợp lệ.", "Error"))
    }
    squareHistory.forEach { lineNum ->
        problems.add(SyntaxProblem(lineNum, "Dấu ngoặc vuông `[` chưa được khép kín bằng `]` hợp lệ.", "Error"))
    }
    
    return problems.distinctBy { it.line to it.message }.sortedBy { it.line }
}

// Generate snippet details for completions
fun getSnippetExpansion(word: String, lang: String): Pair<String, Int> {
    val lowerLang = lang.lowercase()
    return when {
        lowerLang == "kotlin" && word == "fun" -> Pair("fun name() {\n    \n}", 4)
        lowerLang == "kotlin" && word == "if" -> Pair("if (condition) {\n    \n}", 4)
        lowerLang == "kotlin" && word == "for" -> Pair("for (item in list) {\n    \n}", 5)
        lowerLang == "kotlin" && word == "class" -> Pair("class Name {\n    \n}", 6)
        lowerLang == "kotlin" && word == "while" -> Pair("while (condition) {\n    \n}", 7)
        lowerLang == "kotlin" && word == "val" -> Pair("val name = ", 4)
        lowerLang == "kotlin" && word == "var" -> Pair("var name = ", 4)
        
        lowerLang == "html" && word == "div" -> Pair("<div>\n    \n</div>", 5)
        lowerLang == "html" && word == "html" -> Pair("<!DOCTYPE html>\n<html>\n<head>\n    <title>Title</title>\n</head>\n<body>\n    \n</body>\n</html>", 47)
        lowerLang == "html" && word == "script" -> Pair("<script>\n    \n</script>", 9)
        lowerLang == "html" && word == "style" -> Pair("<style>\n    \n</style>", 8)
        lowerLang == "html" && word == "a" -> Pair("<a href=\"#\"></a>", 9)
        
        lowerLang == "css" && word == "flex" -> Pair("display: flex;\njustify-content: center;\nalign-items: center;", 53)
        
        (lowerLang == "javascript" || lowerLang == "typescript") && word == "fun" -> Pair("function name() {\n    \n}", 9)
        (lowerLang == "javascript" || lowerLang == "typescript") && word == "if" -> Pair("if (condition) {\n    \n}", 4)
        (lowerLang == "javascript" || lowerLang == "typescript") && word == "for" -> Pair("for (let i = 0; i < array.length; i++) {\n    \n}", 11)
        
        lowerLang == "python" && word == "def" -> Pair("def function_name():\n    pass", 4)
        lowerLang == "python" && word == "class" -> Pair("class ClassName:\n    def __init__(self):\n        pass", 6)
        
        lowerLang == "java" && word == "fun" -> Pair("public void name() {\n    \n}", 12)
        lowerLang == "java" && word == "class" -> Pair("public class Name {\n    \n}", 13)
        else -> Pair(word, word.length)
    }
}

fun extractLocalIdentifiers(text: String): List<String> {
    val pattern = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
    val matcher = pattern.matcher(text)
    val words = mutableSetOf<String>()
    while (matcher.find()) {
        val word = matcher.group()
        if (word.length > 2 && word !in listOf("fun", "val", "var", "class", "interface", "object", "return", "import", "package", "public", "private", "protected")) {
            words.add(word)
        }
    }
    return words.toList()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MonacoEditor(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var fileToRename by remember { mutableStateOf<FileEntity?>(null) }
    var renameNewName by remember { mutableStateOf("") }

    val isViet = viewModel.isVietnamese
    var fileToDownload by remember { mutableStateOf<FileEntity?>(null) }
    val downloadFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val targetFile = fileToDownload
        if (uri != null && targetFile != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(targetFile.content.toByteArray())
                }
                Toast.makeText(context, if (isViet) "Đã tải xuống thành công!" else "Downloaded successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, if (isViet) "Lỗi tải xuống: ${e.message}" else "Download error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val activeFile = viewModel.activeFile
    val currentText = viewModel.editorText
    val fontSize = viewModel.fontSize
    val isAutoHighlight = viewModel.runAutoHighlight
    val themeColors = ThemeRegistry.getTheme(viewModel.editorTheme)
    
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showAutoCodePanel by remember { mutableStateOf(false) }
    var showProblemsPanel by remember { mutableStateOf(false) }

    if (activeFile == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(themeColors.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                     imageVector = Icons.Default.Code,
                     contentDescription = null,
                     modifier = Modifier.size(64.dp),
                     tint = Color(0xFF388BFD).copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isViet) "Không có tệp nào đang mở" else "No file is currently open",
                    color = themeColors.text.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isViet) "Hãy mở tệp hoặc tải từ GitHub để bắt đầu lập trình" else "Open a file or explore GitHub to start coding",
                    color = themeColors.text.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    } else {
        // Run reactive syntax check on edit smoothly on low-end devices
        val activeLang = viewModel.editorLanguage.lowercase()
        var syntaxProblems by remember { mutableStateOf<List<SyntaxProblem>>(emptyList()) }
        var errorLines by remember { mutableStateOf<Set<Int>>(emptySet()) }

        LaunchedEffect(currentText, activeLang) {
            kotlinx.coroutines.delay(500) // Debounce syntax check
            val problems = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                runSyntaxCheck(currentText, activeLang)
            }
            syntaxProblems = problems
            errorLines = problems.filter { it.severity == "Error" }.map { it.line }.toSet()
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(themeColors.background)
        ) {
            // ------------------ DOUBLE-CLICK RENAME DIALOG ------------------
            if (fileToRename != null) {
                AlertDialog(
                    onDismissRequest = { fileToRename = null },
                    title = { Text(if (isViet) "Đổi tên tệp tin" else "Rename file", color = themeColors.text, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    text = {
                        Column {
                            Text(if (isViet) "Nhập tên mới cho tệp tin này:" else "Enter new name for this file:", color = themeColors.text.copy(alpha = 0.7f), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = renameNewName,
                                onValueChange = { renameNewName = it },
                                label = { Text(if (isViet) "Tên tệp tin mới" else "New file name") },
                                textStyle = TextStyle(color = themeColors.text, fontSize = 14.sp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = themeColors.background,
                                    unfocusedContainerColor = themeColors.background,
                                    focusedTextColor = themeColors.text,
                                    unfocusedTextColor = themeColors.text
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EA043)),
                            onClick = {
                                val target = fileToRename
                                if (target != null && renameNewName.isNotEmpty()) {
                                    viewModel.renameFile(target, renameNewName)
                                }
                                fileToRename = null
                            }
                        ) {
                            Text(if (isViet) "Xác nhận" else "Confirm", color = themeColors.text)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { fileToRename = null }) {
                            Text(if (isViet) "Hủy" else "Cancel", color = Color.LightGray)
                        }
                    },
                    containerColor = themeColors.headerBackground
                )
            }

            // ------------------ MULTI-TAB FLOW ROW ------------------
            val openFiles = viewModel.openFiles
            if (openFiles.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColors.headerBackground)
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(openFiles) { file ->
                        val isActive = file.id == activeFile.id
                        Card(
                            modifier = Modifier
                                .height(36.dp)
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .combinedClickable(
                                    onClick = { viewModel.setFileActive(file) },
                                    onDoubleClick = {
                                        fileToRename = file
                                        renameNewName = file.name
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) themeColors.background else themeColors.headerBackground.copy(alpha = 0.5f)
                            ),
                            border = if (isActive) BorderStroke(1.dp, themeColors.keyword.copy(alpha = 0.3f)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when {
                                        file.name.endsWith(".kt") || file.name.endsWith(".java") -> Icons.Default.Code
                                        file.name.endsWith(".html") || file.name.endsWith(".css") -> Icons.Default.Html
                                        file.name.endsWith(".md") -> Icons.Default.Book
                                        else -> Icons.Default.Description
                                    },
                                    contentDescription = null,
                                    tint = if (isActive) themeColors.keyword else themeColors.text.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = file.name,
                                    color = if (isActive) themeColors.text else themeColors.text.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close File",
                                    tint = if (isActive) themeColors.text.copy(alpha = 0.7f) else themeColors.text.copy(alpha = 0.3f),
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable { viewModel.closeFile(file) }
                                )
                            }
                        }
                    }
                }
                Divider(color = themeColors.lineNumbersText.copy(alpha = 0.15f), thickness = 1.dp)
            }

            // VS Code Style Editor Tab Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = themeColors.headerBackground),
                shape = ShapeDefaults.ExtraSmall
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                         Icon(
                            imageVector = when {
                                activeFile.name.endsWith(".kt") || activeFile.name.endsWith(".java") -> Icons.Default.Code
                                activeFile.name.endsWith(".html") || activeFile.name.endsWith(".css") -> Icons.Default.Html
                                activeFile.name.endsWith(".md") -> Icons.Default.Book
                                else -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = themeColors.keyword,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeFile.name,
                            color = themeColors.text,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        
                        // Dropdown language selection
                        Box {
                            TextButton(
                                onClick = { showLanguageMenu = true },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text(
                                    text = "· ${viewModel.editorLanguage.uppercase()} ▾",
                                    color = themeColors.keyword,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            DropdownMenu(
                                expanded = showLanguageMenu,
                                onDismissRequest = { showLanguageMenu = false },
                                modifier = Modifier.background(themeColors.headerBackground)
                            ) {
                                val popularLanguages = listOf(
                                    "kotlin", "java", "javascript", "typescript", "python", "html", "css", "json", "markdown",
                                    "c", "cpp", "csharp", "go", "rust", "swift", "php", "ruby", "sql", "yaml", "xml", "shell",
                                    "dart", "r", "scala", "haskell", "perl", "lua", "clojure", "groovy", "assembly", "fortran",
                                    "cobol", "fsharp", "elixir", "erlang", "objective-c", "julia", "powershell", "dockerfile",
                                    "batch", "makefile", "apex", "bicep", "coffeescript", "csp", "cypher", "ecl", "graphql",
                                    "handlebars", "hcl", "ini", "less", "lexon", "liquid", "m3", "mips", "msdax", "mysql",
                                    "pascal", "pgsql", "protobuf", "pug", "qsharp", "razor", "redis", "redshift", "restructuredtext",
                                    "sb", "scheme", "scss", "solidity", "sophia", "sparql", "st", "systemverilog", "tcl", "twig",
                                    "vb", "diff", "json5", "jsonc", "toml", "properties", "sas", "ocaml", "nim", "actionscript",
                                    "ada", "clojure", "haxe", "latex", "lisp", "matlab", "verilog", "vhdl", "vue"
                                )
                                popularLanguages.forEach { lang ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = lang.uppercase(),
                                                color = if (viewModel.editorLanguage == lang) themeColors.keyword else themeColors.text,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (viewModel.editorLanguage == lang) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.changeEditorLanguage(lang)
                                            showLanguageMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                fileToDownload = activeFile
                                downloadFileLauncher.launch(activeFile.name)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SaveAlt,
                                contentDescription = if (isViet) "Tải xuống thiết bị" else "Download to device",
                                tint = themeColors.keyword,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.askGeminiToExplainCode() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = if (isViet) "AI Giải Thích" else "AI Explain",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.askGeminiToRefactorCode() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Construction,
                                contentDescription = "AI Refactor",
                                tint = themeColors.keyword,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { showAutoCodePanel = !showAutoCodePanel },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "AI Auto-Code",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.saveSnippetFromEditor() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmarks,
                                contentDescription = if (isViet) "Lưu Snippet" else "Save Snippet",
                                tint = Color(0xFF2EA043),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Divider(color = Color.DarkGray.copy(alpha = 0.3f), thickness = 1.dp)

            // Dynamic suggestions dictionary
            val suggestionsMap = remember {
                mapOf(
                    "kotlin" to listOf(
                        "fun", "val", "var", "class", "interface", "object", "println()", "import", "package", 
                        "return", "if", "else", "when", "for", "while", "Modifier", "Modifier.fillMaxWidth()", 
                        "Modifier.padding()", "Column", "Row", "Box", "Text", "Button", "remember { mutableStateOf() }",
                        "viewModelScope.launch", "launch", "collectAsStateWithLifecycle()", "true", "false", "null"
                    ),
                    "html" to listOf(
                        "<!DOCTYPE html>", "html", "head", "body", "title", "div", "span", "h1", "h2", "p", "a", 
                        "img", "button", "script", "style", "link", "meta", "class=", "id=", "onclick=", "href=", "style="
                    ),
                    "css" to listOf(
                        "background-color: ", "color: ", "font-size: ", "margin: ", "padding: ", "display: flex;", 
                        "align-items: center;", "justify-content: center;", "border-radius: ", "border: ", "width: ", "height: ",
                        "font-family: ", "text-align: center;", "position: absolute;", "z-index: "
                    ),
                    "javascript" to listOf(
                        "const", "let", "var", "function", "console.log()", "document.getElementById()", 
                        "addEventListener", "fetch", "async", "await", "return", "if", "else", "for", "forEach", 
                        "map", "filter", "class", "import", "export", "localStorage", "JSON.stringify()", "JSON.parse()"
                    ),
                    "typescript" to listOf(
                        "const", "let", "function", "interface", "type", "class", "return", "import", "export", 
                        "console.log()", "any", "string", "number", "boolean", "async", "await", "Promise"
                    ),
                    "python" to listOf(
                        "def", "class", "import", "from", "print()", "if", "else", "elif", "for", "while", "in", 
                        "return", "try", "except", "self", "__init__", "list", "dict", "set", "len()", "range()", "True", "False"
                    ),
                    "java" to listOf(
                        "public class", "public static void main(String[] args)", "System.out.println()", "int", "double", "float",
                        "boolean", "char", "String", "import", "package", "new", "return", "if", "else", "for", "while", "class"
                    )
                )
            }

            var textFieldValue by remember(activeFile.id) {
                mutableStateOf(TextFieldValue(currentText))
            }
            if (textFieldValue.text != currentText) {
                textFieldValue = textFieldValue.copy(text = currentText)
            }

            var isSuggestionsEnabled by remember { mutableStateOf(true) }
            var isSuggestionsDetailsExpanded by remember { mutableStateOf(false) }

            LaunchedEffect(viewModel.requestGoToLine) {
                val lineToJump = viewModel.requestGoToLine
                if (lineToJump != null && activeFile != null) {
                    val lines = currentText.split("\n")
                    if (lineToJump >= 1 && lineToJump <= lines.size) {
                        var charIndex = 0
                        for (i in 0 until lineToJump - 1) {
                            charIndex += lines[i].length + 1
                        }
                        textFieldValue = textFieldValue.copy(
                            selection = androidx.compose.ui.text.TextRange(charIndex)
                        )
                    }
                    viewModel.requestGoToLine = null
                }
            }

            val cursorPosition = textFieldValue.selection.start
            val textBeforeCursor = textFieldValue.text.take(cursorPosition)
            val lastWord = remember(textBeforeCursor) {
                textBeforeCursor.split(Regex("[^a-zA-Z0-9_@]")).lastOrNull() ?: ""
            }

            val staticSuggestions = suggestionsMap[activeLang] ?: suggestionsMap["kotlin"]!!
            val localIdentifiers = remember(currentText) { extractLocalIdentifiers(currentText) }

            val availableSuggestions = remember(activeLang, lastWord, localIdentifiers) {
                if (lastWord.isNotEmpty()) {
                    val list = mutableListOf<Pair<String, String>>()
                    // static signatures
                    staticSuggestions.filter { it.startsWith(lastWord, ignoreCase = true) }.forEach {
                        val isSnippet = it in listOf("fun", "if", "for", "while", "class", "def", "div", "script", "style", "html", "val", "var")
                        list.add(Pair(it, if (isSnippet) "⚡" else "🔑"))
                    }
                    // local declared elements
                    localIdentifiers.filter { it.startsWith(lastWord, ignoreCase = true) && it != lastWord }.forEach {
                        list.add(Pair(it, "📦"))
                    }
                    list.distinctBy { it.first }.take(12)
                } else {
                    staticSuggestions.take(6).map { Pair(it, "🔑") }
                }
            }

            var suggestionsSelectedIndex by remember(availableSuggestions) { mutableStateOf(0) }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                
                // Monaco Scrolling code area
                val verticalScrollState = rememberScrollState()
                val horizontalScrollState = rememberScrollState()

                val linesCount = maxOf(1, currentText.split("\n").size)
                val linesText = (1..linesCount).joinToString("\n") { it.toString().padStart(3, ' ') }

                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Gutter Column holding Monaco Line Numbers
                    Column(
                        modifier = Modifier
                            .width(42.dp)
                            .fillMaxHeight()
                            .background(themeColors.lineNumbersBackground)
                            .verticalScroll(verticalScrollState)
                            .padding(top = 12.dp, bottom = 48.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = linesText,
                            color = themeColors.lineNumbersText,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize + 6).sp
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    // Native editable basic text editor container
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(verticalScrollState)
                            .horizontalScroll(horizontalScrollState)
                            .padding(top = 12.dp, bottom = 48.dp, start = 8.dp, end = 16.dp)
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = {
                                textFieldValue = it
                                viewModel.handleTextEdit(it.text)
                            },
                            textStyle = TextStyle(
                                color = themeColors.text,
                                fontFamily = FontFamily.Monospace,
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize + 6).sp
                            ),
                            cursorBrush = SolidColor(themeColors.cursorColor),
                            visualTransformation = if (isAutoHighlight) {
                                CodeSyntaxHighlighter(
                                    language = viewModel.editorLanguage, 
                                    themeName = viewModel.editorTheme,
                                    searchQuery = "",
                                    errorLines = errorLines
                                )
                            } else {
                                CodeSyntaxHighlighter(
                                    language = "plaintext", 
                                    themeName = viewModel.editorTheme,
                                    searchQuery = "",
                                    errorLines = errorLines
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 500.dp)
                        )
                    }
                }

                // AI Auto-Coder Prompt Overlay
                if (showAutoCodePanel) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .align(Alignment.BottomCenter),
                        colors = CardDefaults.cardColors(containerColor = themeColors.headerBackground),
                        border = BorderStroke(1.dp, themeColors.keyword.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isViet) "AI AUTO-CODER (VIẾT TRỰC TIẾP VÀO CON TRỎ)" else "AI AUTO-CODER (WRITE DIRECTLY TO CURSOR)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.text
                                    )
                                }
                                IconButton(
                                    onClick = { showAutoCodePanel = false },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (isViet) "Đóng" else "Close",
                                        tint = themeColors.text.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = viewModel.aiAutoCoderPrompt,
                                onValueChange = { viewModel.aiAutoCoderPrompt = it },
                                placeholder = { Text(if (isViet) "Ví dụ: viết class User, hàm check Prime, CSS Flexbox trung tâm..." else "E.g. write class User, checkPrime function, center CSS Flexbox...", fontSize = 12.sp, color = themeColors.text.copy(alpha = 0.3f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = themeColors.keyword,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = themeColors.text,
                                    unfocusedTextColor = themeColors.text
                                ),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                singleLine = false,
                                maxLines = 3,
                                textStyle = TextStyle(fontSize = 12.sp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (viewModel.isAiAutoCoderGenerating) {
                                    CircularProgressIndicator(
                                        color = themeColors.keyword,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isViet) "AI Đang viết code..." else "AI is writing code...", fontSize = 11.sp, color = themeColors.text.copy(alpha = 0.6f))
                                } else {
                                    Button(
                                        onClick = {
                                            viewModel.generateAndInsertCode { insertedCode ->
                                                val cursor = textFieldValue.selection.start
                                                val currentContent = textFieldValue.text
                                                val newContent = currentContent.substring(0, cursor) + insertedCode + currentContent.substring(cursor)
                                                val newCursorRange = androidx.compose.ui.text.TextRange(cursor + insertedCode.length)
                                                
                                                textFieldValue = TextFieldValue(text = newContent, selection = newCursorRange)
                                                viewModel.handleTextEdit(newContent)
                                                showAutoCodePanel = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.keyword),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.AutoMode, contentDescription = null, modifier = Modifier.size(14.dp), tint = themeColors.text)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isViet) "Bắt đầu & Chèn" else "Generate & Insert", fontSize = 11.sp, color = themeColors.text, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }


                // Floating vertical Monaco style IntelliSense popup removed from inside the scroll container to prevent screen blockage

                // Interactive diagnostics details list popup (uncollapsed when clicking status problems)
                if (showProblemsPanel) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(8.dp)
                            .align(Alignment.BottomCenter),
                        colors = CardDefaults.cardColors(containerColor = themeColors.headerBackground),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isViet) "BẢN TIN CHẨN ĐOÁN LỖI CÚ PHÁP (PROBLEMS)" else "SYNTAX DIAGNOSTIC REPORT (PROBLEMS)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColors.text)
                                }
                                IconButton(onClick = { showProblemsPanel = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = if (isViet) "Đóng" else "Close", tint = themeColors.text.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                }
                            }
                            Divider(color = Color.DarkGray.copy(alpha = 0.2f))

                            if (syntaxProblems.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(if (isViet) "Tuyệt vời! Không phát hiện lỗi cú pháp nào." else "Excellent! No syntax errors detected.", fontSize = 11.sp, color = Color.Gray)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    itemsIndexed(syntaxProblems) { _, problem ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    // Jump directly to that line inside the editor!
                                                    val lines = currentText.split("\n")
                                                    if (problem.line >= 1 && problem.line <= lines.size) {
                                                        var charIndex = 0
                                                        for (i in 0 until problem.line - 1) {
                                                            charIndex += lines[i].length + 1
                                                        }
                                                        textFieldValue = textFieldValue.copy(
                                                            selection = androidx.compose.ui.text.TextRange(charIndex)
                                                        )
                                                    }
                                                    showProblemsPanel = false
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (problem.severity == "Error") Color(0xFFD32F2F) else Color(0xFFF57C00))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isViet) "Dòng ${problem.line}" else "Line ${problem.line}",
                                                    color = themeColors.text,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = problem.message,
                                                color = themeColors.text,
                                                fontSize = 11.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            var forceShowSymbols by remember(activeFile.id) { mutableStateOf(false) }

            val showSuggestions = isSuggestionsEnabled && availableSuggestions.isNotEmpty() && lastWord.isNotEmpty()

            // Unified ergonomic Accessory/Suggestion Bar (Only one bar)
            Surface(
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.DarkGray.copy(alpha = 0.2f)),
                color = themeColors.headerBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left sticky action control
                    if (showSuggestions) {
                        IconButton(
                            onClick = { forceShowSymbols = !forceShowSymbols },
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (forceShowSymbols) Color.Transparent else themeColors.keyword.copy(alpha = 0.15f),
                                    RoundedCornerShape(6.dp)
                                )
                        ) {
                            Icon(
                                imageVector = if (forceShowSymbols) Icons.Default.Bolt else Icons.Default.Code,
                                contentDescription = if (forceShowSymbols) "Xem Gợi ý" else "Xem Ký tự đặc biệt",
                                tint = if (forceShowSymbols) Color(0xFFFF9800) else themeColors.keyword,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        // Icon to toggle active auto symbol suggestions globally
                        IconButton(
                            onClick = { isSuggestionsEnabled = !isSuggestionsEnabled },
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (isSuggestionsEnabled) themeColors.keyword.copy(alpha = 0.1f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                        ) {
                            Icon(
                                imageVector = if (isSuggestionsEnabled) Icons.Default.Bolt else Icons.Default.Close,
                                contentDescription = "Bật/Tắt Gợi ý",
                                tint = if (isSuggestionsEnabled) Color(0xFFFF9800) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.Gray.copy(alpha = 0.3f)))
                    Spacer(modifier = Modifier.width(6.dp))

                    // Scrollable dynamic content ribbon (Suggestions OR Symbols)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (showSuggestions && !forceShowSymbols) {
                            // Render suggestions with Single-Tap Completion!
                            availableSuggestions.forEachIndexed { index, (value, iconType) ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(themeColors.background)
                                        .border(
                                            width = 1.dp,
                                            color = themeColors.keyword.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            // SINGLE TAP - INSTANT AUTOCOMPLETE!
                                            val (expandedValue, cursorShift) = getSnippetExpansion(value, activeLang)
                                            val newTextBeforeCursor = textBeforeCursor.dropLast(lastWord.length) + expandedValue
                                            val newTextAfterCursor = textFieldValue.text.substring(cursorPosition)
                                            val newTotalText = newTextBeforeCursor + newTextAfterCursor
                                            val newCursorPosition = textBeforeCursor.length - lastWord.length + cursorShift

                                            textFieldValue = TextFieldValue(
                                                text = newTotalText,
                                                selection = androidx.compose.ui.text.TextRange(newCursorPosition)
                                            )
                                            viewModel.handleTextEdit(newTotalText)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when(iconType) {
                                            "⚡" -> "⚡ "
                                            "🔑" -> "🔑 "
                                            "📦" -> "📦 "
                                            else -> "🛠️ "
                                        },
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = value,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = themeColors.keyword,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // Render standard high-density quick symbols + arrows
                            IconButton(
                                onClick = { viewModel.undo() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(themeColors.background, RoundedCornerShape(4.dp))
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = "Undo", tint = themeColors.text, modifier = Modifier.size(14.dp))
                            }
                            IconButton(
                                onClick = { viewModel.redo() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(themeColors.background, RoundedCornerShape(4.dp))
                            ) {
                                Icon(Icons.Default.Redo, contentDescription = "Redo", tint = themeColors.text, modifier = Modifier.size(14.dp))
                            }

                            IconButton(
                                onClick = {
                                    val newCursor = maxOf(0, textFieldValue.selection.start - 1)
                                    textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange(newCursor))
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(themeColors.background, RoundedCornerShape(4.dp))
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Trái", tint = themeColors.text, modifier = Modifier.size(14.dp))
                            }

                            IconButton(
                                onClick = {
                                    val newCursor = minOf(textFieldValue.text.length, textFieldValue.selection.start + 1)
                                    textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange(newCursor))
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(themeColors.background, RoundedCornerShape(4.dp))
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Phải", tint = themeColors.text, modifier = Modifier.size(14.dp))
                            }
                            
                            Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.Gray.copy(alpha = 0.3f)))

                            val accessorySymbols = listOf(
                                "Tab" to "\t", "{" to "{}", "}" to "}", "(" to "()", ")" to ")",
                                "[" to "[]", "]" to "]", "<" to "<>", ">" to ">",
                                "\"" to "\"\"", "'" to "''", ";" to ";", "=" to " = ",
                                ":" to ": ", "_" to "_", "." to ".", "+" to " + ",
                                "-" to " - ", "*" to " * ", "/" to " / ", "$" to "$"
                            )

                            accessorySymbols.forEach { (label, value) ->
                                Button(
                                    onClick = {
                                        val cursor = textFieldValue.selection.start
                                        val code = textFieldValue.text
                                        val isPaired = value in listOf("{}", "()", "[]", "<>", "\"\"", "''")
                                        val insertVal = if (label == "Tab") "    " else value
                                        val newText = code.substring(0, cursor) + insertVal + code.substring(cursor)
                                        val newCursor = if (isPaired) cursor + 1 else cursor + insertVal.length
                                        
                                        textFieldValue = TextFieldValue(text = newText, selection = androidx.compose.ui.text.TextRange(newCursor))
                                        viewModel.handleTextEdit(newText)
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 32.dp, minWidth = 36.dp)
                                        .height(32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.background, contentColor = themeColors.text),
                                    shape = RoundedCornerShape(6.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (label in listOf("Tab", "{", "}", "(", ")", "[", "]")) themeColors.keyword else themeColors.text
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // VSCode-Style high density reactive Status Bar at the bottom (Hidden when keyboard is active to maximize typing viewport)
            if (!WindowInsets.isImeVisible) {
                val textCursor = textFieldValue.selection.start
                val textBefore = textFieldValue.text.take(textCursor)
            val currentLine = textBefore.count { it == '\n' } + 1
            val currentColumn = textCursor - textBefore.lastIndexOf('\n')
            val selectionLen = Math.abs(textFieldValue.selection.end - textFieldValue.selection.start)
            val errorsCount = syntaxProblems.filter { it.severity == "Error" }.size
            val warningsCount = syntaxProblems.filter { it.severity == "Warning" }.size

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = themeColors.headerBackground,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left area: Interactive Problems panel trigger
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showProblemsPanel = !showProblemsPanel }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = if (errorsCount > 0) Color(0xFFF85149) else Color.LightGray, modifier = Modifier.size(11.dp))
                            Text("$errorsCount", color = if (errorsCount > 0) Color(0xFFF85149) else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = if (warningsCount > 0) Color(0xFFCCA700) else Color.LightGray, modifier = Modifier.size(11.dp))
                            Text("$warningsCount", color = if (warningsCount > 0) Color(0xFFCCA700) else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        
                        Text(
                            text = "Problems",
                            color = themeColors.text.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Right area: Ln/Col data metrics & Language
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (selectionLen > 0) {
                            Text(
                                text = if (isViet) "Col chọn: $selectionLen ký tự" else "Selected: $selectionLen chars",
                                color = themeColors.text.copy(alpha = 0.5f),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = if (isViet) "Dòng $currentLine, Cột $currentColumn" else "Ln $currentLine, Col $currentColumn",
                            color = themeColors.text.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = viewModel.editorLanguage.uppercase(),
                            color = themeColors.keyword,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "UTF-8",
                            color = themeColors.text.copy(alpha = 0.4f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            }
        }
    }
}
