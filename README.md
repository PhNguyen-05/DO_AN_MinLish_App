# DO_AN_MinLish_App
# MinLish App - Ứng dụng hỗ trợ học từ vựng tiếng Anh

MinLish là ứng dụng hỗ trợ học từ vựng tiếng Anh áp dụng phương pháp lặp lại ngắt quãng (Spaced Repetition - Thuật toán SM-2) kết hợp Flashcard trực quan. Dự án được phát triển theo kiến trúc MVVM tiêu chuẩn và tổ chức thư mục theo tính năng (Package by Feature).

## 🛠️ Công nghệ sử dụng

- **Frontend:** Android Studio, Kotlin, Jetpack Compose, Retrofit, Coil (nạp ảnh).
- **Backend:** Node.js, Express REST API, JWT Authentication, Bcrypt (băm mật khẩu).
- **Database:** MySQL.

---

## 📁 Cấu trúc thư mục dự án

```text
DOAN1/
├── backend/               # Mã nguồn Server Node.js
│   ├── node_modules/      # Thư viện Node.js (được bỏ qua bởi .gitignore)
│   ├── server.js          # File chạy chính của Server API
│   └── .gitignore
│
└── frontend/              # Mã nguồn Ứng dụng Android Studio
    └── app/src/main/java/com/minlish/app/
        ├── data/          # Tầng dữ liệu dùng chung (Model, ApiService, Session)
        └── feature/       # Tầng giao diện và logic chia theo tính năng (MVVM)
            ├── auth/      # Tính năng Đăng nhập & Đăng ký (Login, Register, ViewModel)
            └── home/      # Tính năng Trang chủ (HomeScreen, HomeViewModel)
