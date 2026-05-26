const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/ui/components/SettingsPanel.kt', 'utf8');
const reps = [
['if (isViet) "THIẾT LẬP HỆ THỐNG" else "SYSTEM CONFIGURATION"', 'if (isViet) "THIẾT LẬP APP" else "APP SETTINGS"'],
['if (isViet) "NGÔN NGỮ HỆ THỐNG / LANGUAGE" else "SYSTEM LANGUAGE / NGÔN NGỮ"', 'if (isViet) "NGÔN NGỮ / LANGUAGE" else "LANGUAGE"'],
['if (isViet) "GIAO DIỆN CHỦ ĐỀ CHUYÊN NGHIỆP (THEME)" else "PROFESSIONAL COSMETIC THEME"', 'if (isViet) "GIAO DIỆN CHỦ ĐỀ" else "EDITOR THEME"'],
['if (isViet) "KÍCH THƯỚC PHÔNG CHỮ EDITOR" else "EDITOR FONT DIAMETER SIZE"', 'if (isViet) "KÍCH THƯỚC CHỮ" else "FONT SIZE"'],
['if (isViet) "HIGHLIGHT CÚ PHÁP TỰ ĐỘNG" else "AUTOMATIC SYNTAX HIGHLIGHTING"', 'if (isViet) "NỔI BẬT CÚ PHÁP" else "SYNTAX HIGHLIGHTING"'],
['if (isViet) "Tự động tô màu mã nguồn thông minh" else "Real-time aesthetic parsing styling highlights"', 'if (isViet) "Tô màu mã nguồn cục bộ." else "Colorize code syntax based on language."'],
['if (isViet) "Cài đặt nâng cao & Tích hợp" else "Advanced & Integrations"', 'if (isViet) "Tích hợp nâng cao" else "Advanced Integrations"'],
['if (isViet) "CẤU HÌNH GEMINI API KEY (AI CHUYÊN GIA)" else "GEMINI COGNITIVE API KEY KEYWORDS"', 'if (isViet) "GEMINI API KEY" else "GEMINI API KEY"'],
['if (isViet) "Nhập API Key của bạn để trò chuyện và tối ưu code tự do." else "Introduce personal APIs keys directly to empower the Gemini AI developer assistant."', 'if (isViet) "Sử dụng key để dùng tính năng AI." else "Required for AI assistant."'],
['if (isViet) "Mã khoá Gemini API Key..." else "Gemini API Token Access..."', 'if (isViet) "Nhập Gemini API Key..." else "Enter Gemini API Key..."'],
['if (isViet) "CẤU HÌNH GITHUB USERNAME" else "GITHUB ACCOUNT USERNAME LINKING"', 'if (isViet) "GITHUB USERNAME" else "GITHUB USERNAME"'],
['if (isViet) "Liên kết tài khoản GitHub của bạn để dễ dàng duyệt Repo." else "Bind your user handle directly to explore your remote repositories seamlessly."', 'if (isViet) "Tra cứu kho lưu trữ trên xa." else "Explore remote Github repositories."'],
['if (isViet) "CẤU HÌNH GITHUB PERSONAL ACCESS TOKEN" else "GITHUB ACCESS PERSONAL PRIVILEGED TOKEN"', 'if (isViet) "GITHUB PERSONAL ACCESS TOKEN" else "GITHUB PERSONAL ACCESS TOKEN"'],
['if (isViet) "Thêm mã Token để tránh bị giới hạn lượt gọi APIs của GitHub." else "Attach personal token ghp_ to support push commits and avoid API throttling."', 'if (isViet) "Tăng giới hạn API và cho phép git push." else "Enhance API rate limits and allow push."'],
['if (isViet) "Mã truy cập ghp_..." else "Github Access Token ghp_..."', 'if (isViet) "Nhập Token (ghp_...)" else "Enter Token (ghp_...)"'],
['if (isViet) "SAO LƯU & PHỤC HỒI CẤU HÌNH" else "RESTORE & CONFIGURATION BACKUP SYNC"', 'if (isViet) "SAO LƯU DỮ LIỆU" else "DATA BACKUP"'],
['if (isViet) "Sao chép mã cấu hình đề phòng khi cài lại ứng dụng, hoặc tải từ file đã lưu." else "Export database keys or restore from a saved file to dynamically recover setups."', 'if (isViet) "Xuất/nhập hồ sơ và dự án (JSON)." else "Export/Import profiles and setup (JSON)."'],
['if (isViet) "Lưu File JSON" else "Save JSON File"', 'if (isViet) "Xuất (Export)" else "Export"'],
['if (isViet) "Mở File JSON" else "Open JSON File"', 'if (isViet) "Nhập (Import)" else "Import"']
];
for (const r of reps) code = code.replace(r[0], r[1]);
fs.writeFileSync('app/src/main/java/com/example/ui/components/SettingsPanel.kt', code);