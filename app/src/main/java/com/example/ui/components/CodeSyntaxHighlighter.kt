package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import java.util.regex.Pattern

data class ThemeColors(
    val background: Color,
    val headerBackground: Color,
    val text: Color,
    val keyword: Color,
    val type: Color,
    val string: Color,
    val comment: Color,
    val number: Color,
    val annotation: Color,
    val htmlTag: Color,
    val lineNumbersBackground: Color = Color(0xFF090D14),
    val lineNumbersText: Color = Color.Gray.copy(alpha = 0.5f),
    val cursorColor: Color = Color(0xFF58A6FF),
    val operatorColor: Color = Color(0xFFFF7B72),
    val punctuationColor: Color = Color(0xFFE6EDF0),
    val functionColor: Color = Color(0xFFDCDCAA)
)

object ThemeRegistry {
    val themes = mapOf(
        "Github Dark" to ThemeColors(
            background = Color(0xFF0F141C),
            headerBackground = Color(0xFF161B22),
            text = Color(0xFFE6EDF0),
            keyword = Color(0xFF58A6FF),
            type = Color(0xFF4EC9B0),
            string = Color(0xFFCE9178),
            comment = Color(0xFF8B949E),
            number = Color(0xFF79C0FF),
            annotation = Color(0xFFFF7B72),
            htmlTag = Color(0xFFFF7B72),
            operatorColor = Color(0xFFFF7B72),
            punctuationColor = Color(0xFF8B949E),
            functionColor = Color(0xFFDCDCAA),
            lineNumbersBackground = Color(0xFF0F141C),
            cursorColor = Color(0xFF58A6FF)
        ),
        "Dracula" to ThemeColors(
            background = Color(0xFF282A36),
            headerBackground = Color(0xFF1E1F29),
            text = Color(0xFFF8F8F2),
            keyword = Color(0xFFFF79C6),
            type = Color(0xFF8BE9FD),
            string = Color(0xFF50FA7B),
            comment = Color(0xFF6272A4),
            number = Color(0xFFBD93F9),
            annotation = Color(0xFFF1FA8C),
            htmlTag = Color(0xFFFF79C6),
            operatorColor = Color(0xFFFF79C6),
            punctuationColor = Color(0xFFF8F8F2),
            functionColor = Color(0xFF50FA7B),
            lineNumbersBackground = Color(0xFF1E1F29),
            cursorColor = Color(0xFFF8F8F0)
        ),
        "One Dark" to ThemeColors(
            background = Color(0xFF21252B),
            headerBackground = Color(0xFF1E2127),
            text = Color(0xFFABB2BF),
            keyword = Color(0xFFC678DD),
            type = Color(0xFF56B6C2),
            string = Color(0xFF98C379),
            comment = Color(0xFF5C6370),
            number = Color(0xFFD19A66),
            annotation = Color(0xFFE5C07B),
            htmlTag = Color(0xFFE06C75),
            operatorColor = Color(0xFF56B6C2),
            punctuationColor = Color(0xFFABB2BF),
            functionColor = Color(0xFF61AFEF),
            lineNumbersBackground = Color(0xFF1E2127),
            cursorColor = Color(0xFF528BFF)
        ),
        "Monokai" to ThemeColors(
            background = Color(0xFF272822),
            headerBackground = Color(0xFF1E1F1C),
            text = Color(0xFFF8F8F2),
            keyword = Color(0xFFF92672),
            type = Color(0xFF66D9EF),
            string = Color(0xFFE6DB74),
            comment = Color(0xFF75715E),
            number = Color(0xFFAE81FF),
            annotation = Color(0xFFA6E22E),
            htmlTag = Color(0xFFF92672),
            operatorColor = Color(0xFFF92672),
            punctuationColor = Color(0xFFF8F8F2),
            functionColor = Color(0xFFA6E22E),
            lineNumbersBackground = Color(0xFF1E1F1C),
            cursorColor = Color(0xFFF8F8F0)
        ),
        "Nord" to ThemeColors(
            background = Color(0xFF2E3440),
            headerBackground = Color(0xFF161B22),
            text = Color(0xFFD8DEE9),
            keyword = Color(0xFF81A1C1),
            type = Color(0xFF88C0D0),
            string = Color(0xFFA3BE8C),
            comment = Color(0xFF4C566A),
            number = Color(0xFFB48EAD),
            annotation = Color(0xFFEBCB8B),
            htmlTag = Color(0xFF81A1C1),
            operatorColor = Color(0xFF81A1C1),
            punctuationColor = Color(0xFFD8DEE9),
            functionColor = Color(0xFF88C0D0),
            lineNumbersBackground = Color(0xFF161B22),
            cursorColor = Color(0xFF88C0D0)
        ),
        "Solarized Light" to ThemeColors(
            background = Color(0xFFFDF6E3),
            headerBackground = Color(0xFFEEE8D5),
            text = Color(0xFF586E75),
            keyword = Color(0xFF859900),
            type = Color(0xFF268BD2),
            string = Color(0xFF2AA198),
            comment = Color(0xFF93A1A1),
            number = Color(0xFFD33682),
            annotation = Color(0xFFB58900),
            htmlTag = Color(0xFFCB4B16),
            operatorColor = Color(0xFF93A1A1),
            punctuationColor = Color(0xFF586E75),
            functionColor = Color(0xFF268BD2),
            lineNumbersBackground = Color(0xFFEEE8D5),
            lineNumbersText = Color(0xFF93A1A1),
            cursorColor = Color(0xFF268BD2)
        ),
        "Tokyo Night" to createCustomTheme(Color(0xFF1A1B26), Color(0xFF7AA2F7), Color(0xFFA9B1D6)),
        "Cyberpunk" to createCustomTheme(Color(0xFF0D0E15), Color(0xFF00F0FF), Color(0xFFFF0055)),
        "Green Matrix" to createCustomTheme(Color(0xFF010A01), Color(0xFF39FF14), Color(0xFFA8FFA8)),
        "Warm Gruv" to createCustomTheme(Color(0xFF1E1E1E), Color(0xFFFE8019), Color(0xFFEBDBB2)),
        "Soft Mint" to createCustomTheme(Color(0xFF181D1A), Color(0xFF4E9A70), Color(0xFFD3DFD8))
    )

    private fun createCustomTheme(bg: Color, kw: Color, txt: Color): ThemeColors {
        val isLight = (0.2126f * bg.red + 0.7152f * bg.green + 0.0722f * bg.blue) > 0.5f
        val headerBg = if (isLight) {
            Color(
                red = (bg.red * 0.85f).coerceIn(0f, 1f),
                green = (bg.green * 0.85f).coerceIn(0f, 1f),
                blue = (bg.blue * 0.85f).coerceIn(0f, 1f),
                alpha = bg.alpha
            )
        } else {
            Color(
                red = (bg.red + (1f - bg.red) * 0.15f).coerceIn(0f, 1f),
                green = (bg.green + (1f - bg.green) * 0.15f).coerceIn(0f, 1f),
                blue = (bg.blue + (1f - bg.blue) * 0.15f).coerceIn(0f, 1f),
                alpha = bg.alpha
            )
        }
        val lineNumbersBg = if (isLight) {
            Color(
                red = (bg.red * 0.92f).coerceIn(0f, 1f),
                green = (bg.green * 0.92f).coerceIn(0f, 1f),
                blue = (bg.blue * 0.92f).coerceIn(0f, 1f),
                alpha = bg.alpha
            )
        } else {
            Color(
                red = (bg.red + (1f - bg.red) * 0.08f).coerceIn(0f, 1f),
                green = (bg.green + (1f - bg.green) * 0.08f).coerceIn(0f, 1f),
                blue = (bg.blue + (1f - bg.blue) * 0.08f).coerceIn(0f, 1f),
                alpha = bg.alpha
            )
        }
        return ThemeColors(
            background = bg,
            headerBackground = headerBg,
            text = txt,
            keyword = kw,
            type = Color(0xFFf5e0dc),
            string = Color(0xFFa6e3a1),
            comment = Color(0xFF6c7086),
            number = Color(0xFFfab387),
            annotation = kw,
            htmlTag = kw,
            operatorColor = kw,
            punctuationColor = txt,
            functionColor = kw,
            lineNumbersBackground = lineNumbersBg,
            cursorColor = kw
        )
    }

    fun getTheme(name: String): ThemeColors {
        return themes[name] ?: themes["Github Dark"]!!
    }
}

class CodeSyntaxHighlighter(
    private val language: String = "kotlin",
    private val themeName: String = "Github Dark",
    private val searchQuery: String = "",
    private val errorLines: Set<Int> = emptySet()
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            highlight(text.text, language, themeName, searchQuery, errorLines),
            OffsetMapping.Identity
        )
    }

    companion object {
        // Detailed keyword tables per language
        private val keywordsByLang = mapOf(
            "kotlin" to setOf(
                "package", "import", "class", "interface", "object", "fun", "val", "var",
                "return", "if", "else", "for", "while", "do", "when", "is", "in", "as",
                "try", "catch", "finally", "throw", "this", "super", "null", "true", "false",
                "open", "abstract", "override", "private", "public", "protected", "internal",
                "companion", "data", "sealed", "constructor", "init", "suspend", "coroutine", 
                "by", "lazy", "lateinit", "inline", "tailrec", "operator", "infix", "get", "set"
            ),
            "java" to setOf(
                "package", "import", "public", "private", "protected", "class", "interface",
                "extends", "implements", "vola", "transient", "new", "return", "if", "else",
                "for", "while", "do", "switch", "case", "default", "break", "continue", "try",
                "catch", "finally", "throw", "throws", "this", "super", "null", "true", "false",
                "void", "static", "final", "abstract", "synchronized", "volatile", "strictfp",
                "instanceof", "enum", "assert"
            ),
            "javascript" to setOf(
                "import", "export", "default", "from", "as", "const", "let", "var", "function",
                "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue",
                "try", "catch", "finally", "throw", "this", "null", "undefined", "true", "false",
                "class", "extends", "super", "new", "async", "await", "yield", "of", "in", "typeof",
                "instanceof", "delete", "debugger", "static", "get", "set"
            ),
            "typescript" to setOf(
                "import", "export", "default", "from", "as", "const", "let", "var", "function",
                "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue",
                "try", "catch", "finally", "throw", "this", "null", "undefined", "true", "false",
                "class", "extends", "super", "new", "async", "await", "yield", "of", "in", "typeof",
                "instanceof", "delete", "static", "get", "set", "interface", "type", "namespace",
                "declare", "keyof", "readonly", "private", "public", "protected", "abstract", "implements"
            ),
            "python" to setOf(
                "def", "class", "return", "if", "else", "elif", "for", "while", "break", "continue",
                "try", "except", "finally", "raise", "import", "from", "as", "in", "is", "and", "or",
                "not", "lambda", "global", "nonlocal", "pass", "with", "assert", "del", "yield",
                "True", "False", "None"
            ),
            "c" to setOf(
                "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
                "else", "enum", "extern", "float", "for", "goto", "if", "int", "long", "register",
                "return", "short", "signed", "sizeof", "static", "struct", "switch", "typedef",
                "union", "unsigned", "void", "volatile", "while"
            ),
            "cpp" to setOf(
                "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
                "else", "enum", "extern", "float", "for", "goto", "if", "int", "long", "register",
                "return", "short", "signed", "sizeof", "static", "struct", "switch", "typedef",
                "union", "unsigned", "void", "volatile", "while", "class", "namespace", "using",
                "friend", "public", "private", "protected", "new", "delete", "throw", "try", "catch",
                "operator", "template", "typename", "virtual", "inline", "explicit", "mutable", "noexcept"
            ),
            "go" to setOf(
                "break", "default", "func", "interface", "select", "case", "defer", "go", "map",
                "struct", "chan", "else", "goto", "package", "switch", "const", "fallthrough", "if",
                "range", "type", "continue", "for", "import", "return", "var"
            ),
            "rust" to setOf(
                "as", "break", "const", "continue", "crate", "else", "enum", "extern", "false", "fn",
                "for", "if", "impl", "in", "let", "loop", "match", "mod", "move", "mut", "pub", "ref",
                "return", "self", "Self", "static", "struct", "super", "trait", "true", "type", "unsafe",
                "use", "where", "while", "async", "await", "dyn"
            ),
            "sql" to setOf(
                "select", "insert", "update", "delete", "from", "where", "and", "or", "not", "join",
                "left", "right", "inner", "outer", "on", "group", "by", "having", "order", "asc", "desc",
                "create", "table", "alter", "drop", "index", "view", "primary", "key", "foreign", "references",
                "into", "values", "set", "as", "is", "null", "distinct", "union", "all", "limit", "offset"
            )
        )

        fun highlight(code: String, lang: String, themeName: String, searchQuery: String, errorLines: Set<Int>): AnnotatedString {
            val builder = AnnotatedString.Builder(code)
            val lowerLang = lang.lowercase()
            val colors = ThemeRegistry.getTheme(themeName)

            // 1. Operators & Punctuation
            val operatorPattern = Pattern.compile("[+\\-*/%=<>!&|^~?:]")
            val operatorMatcher = operatorPattern.matcher(code)
            while (operatorMatcher.find()) {
                builder.addStyle(
                    SpanStyle(color = colors.operatorColor),
                    operatorMatcher.start(),
                    operatorMatcher.end()
                )
            }

            val punctuationPattern = Pattern.compile("[\\{\\}\\[\\]\\(\\);,.]")
            val punctuationMatcher = punctuationPattern.matcher(code)
            while (punctuationMatcher.find()) {
                builder.addStyle(
                    SpanStyle(color = colors.punctuationColor),
                    punctuationMatcher.start(),
                    punctuationMatcher.end()
                )
            }

            // 2. Numbers
            val numberPattern = Pattern.compile("\\b0x[0-9a-fA-F]+\\b|\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b")
            val numberMatcher = numberPattern.matcher(code)
            while (numberMatcher.find()) {
                builder.addStyle(
                    SpanStyle(color = colors.number),
                    numberMatcher.start(),
                    numberMatcher.end()
                )
            }

            // 3. Highlight class declarations dynamically
            val classDeclPattern = Pattern.compile("\\b(class|interface|struct|enum|object|union)\\s+([a-zA-Z_][a-zA-Z0-9_]*)")
            val classDeclMatcher = classDeclPattern.matcher(code)
            while (classDeclMatcher.find()) {
                val typeStart = classDeclMatcher.start(2)
                val typeEnd = classDeclMatcher.end(2)
                if (typeStart != -1 && typeEnd != -1) {
                    builder.addStyle(
                        SpanStyle(color = colors.type, fontWeight = FontWeight.Bold),
                        typeStart,
                        typeEnd
                    )
                }
            }

            // 4. Highlight function calls dynamically
            val fnCallPattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b(?=\\s*\\()")
            val fnCallMatcher = fnCallPattern.matcher(code)
            while (fnCallMatcher.find()) {
                builder.addStyle(
                    SpanStyle(color = colors.functionColor),
                    fnCallMatcher.start(),
                    fnCallMatcher.end()
                )
            }

            // 5. Annotations (@...)
            val annotationPattern = Pattern.compile("@[A-Za-z0-9_]+")
            val annotationMatcher = annotationPattern.matcher(code)
            while (annotationMatcher.find()) {
                builder.addStyle(
                    SpanStyle(color = colors.annotation, fontWeight = FontWeight.SemiBold),
                    annotationMatcher.start(),
                    annotationMatcher.end()
                )
            }

            // 6. Keywords
            if (lowerLang == "html" || lowerLang == "xml") {
                // HTML Tags
                val tagPattern = Pattern.compile("<[^>]+>")
                val tagMatcher = tagPattern.matcher(code)
                while (tagMatcher.find()) {
                    builder.addStyle(
                        SpanStyle(color = colors.htmlTag, fontWeight = FontWeight.Bold),
                        tagMatcher.start(),
                        tagMatcher.end()
                    )
                }
            } else {
                val keywords = keywordsByLang[lowerLang] ?: keywordsByLang["kotlin"]!!
                val keywordPattern = Pattern.compile("\\b(" + keywords.joinToString("|") + ")\\b")
                val keywordMatcher = keywordPattern.matcher(code)
                while (keywordMatcher.find()) {
                    builder.addStyle(
                        SpanStyle(color = colors.keyword, fontWeight = FontWeight.SemiBold),
                        keywordMatcher.start(),
                        keywordMatcher.end()
                    )
                }

                // General Types mapping
                val typePattern = Pattern.compile("\\b(String|Int|Boolean|Double|Float|Long|Char|Short|Byte|Any|Unit|List|Map|Set|var|let|const|Promise|void|static|dynamic|Array)\\b")
                val typeMatcher = typePattern.matcher(code)
                while (typeMatcher.find()) {
                    builder.addStyle(
                        SpanStyle(color = colors.type),
                        typeMatcher.start(),
                        typeMatcher.end()
                    )
                }
            }

            // 7. Strings (Override preceding keywords/brackets styles)
            val stringPattern = Pattern.compile("\"(\\\\.|[^\"\\\\])*\"|'(\\\\.|[^'\\\\])*'|`(\\\\.|[^`\\\\])*`")
            val stringMatcher = stringPattern.matcher(code)
            while (stringMatcher.find()) {
                builder.addStyle(
                    SpanStyle(color = colors.string),
                    stringMatcher.start(),
                    stringMatcher.end()
                )
            }

            // 8. Comments (Override EVERYTHING)
            val commentPattern = if (lowerLang == "python" || lowerLang == "yaml" || lowerLang == "properties") {
                Pattern.compile("#.*")
            } else {
                Pattern.compile("(//.*)|(/\\*.*?\\*/)", Pattern.DOTALL)
            }
            val commentMatcher = commentPattern.matcher(code)
            while (commentMatcher.find()) {
                builder.addStyle(
                    SpanStyle(color = colors.comment),
                    commentMatcher.start(),
                    commentMatcher.end()
                )
            }

            // 9. Error wavy underlays
            if (errorLines.isNotEmpty()) {
                val lines = code.split("\n")
                var accumulatedChars = 0
                lines.forEachIndexed { index, line ->
                    val lineNum = index + 1
                    if (lineNum in errorLines) {
                        val firstNonWhitespace = line.indexOfFirst { !it.isWhitespace() }
                        val start = accumulatedChars + if (firstNonWhitespace != -1) firstNonWhitespace else 0
                        val end = accumulatedChars + line.length
                        if (start < end) {
                            builder.addStyle(
                                SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                    color = Color(0xFFF85149),
                                    fontWeight = FontWeight.Bold
                                ),
                                start,
                                end
                            )
                        }
                    }
                    accumulatedChars += line.length + 1
                }
            }

            // 10. Active Search Highlights
            if (searchQuery.isNotEmpty()) {
                val searchPattern = Pattern.compile(Pattern.quote(searchQuery), Pattern.CASE_INSENSITIVE)
                val searchMatcher = searchPattern.matcher(code)
                while (searchMatcher.find()) {
                    builder.addStyle(
                        SpanStyle(
                            background = Color(0x90FFD700),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        ),
                        searchMatcher.start(),
                        searchMatcher.end()
                    )
                }
            }

            return builder.toAnnotatedString()
        }
    }
}
