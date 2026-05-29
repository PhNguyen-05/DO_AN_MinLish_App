const express = require('express');
const mysql = require('mysql2/promise');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const cors = require('cors');

const app = express();

app.use('/static', express.static('public'));

app.use(express.json());
app.use(cors());

// Mã khóa bí mật để mã hóa và giải mã JWT Token
const JWT_SECRET = "minlish_super_secret_key_2026";

// Cấu hình kết nối MySQL Database kết hợp Connection Pool tối ưu hiệu năng
const db = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '123456', // Thay bằng mật khẩu MySQL chuẩn trên máy của bạn
    database: 'minlish_db',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

// ==========================================
// 1. API ĐĂNG KÝ TÀI KHOẢN MỚI
// ==========================================
app.post('/api/auth/register', async (req, res) => {
    // Hứng chính xác các biến dạng CamelCase gửi lên từ tầng Frontend Kotlin
    const { email, passwordHash, fullName, targetGoal } = req.body;

    if (!email || !passwordHash || !fullName) {
        return res.status(400).json({ message: 'Vui lòng nhập đầy đủ các trường thông tin bắt buộc!' });
    }

    try {
        // Kiểm tra xem Email đã có ai đăng ký trước đó chưa
        const [existingUsers] = await db.query('SELECT id FROM users WHERE email = ?', [email]);
        if (existingUsers.length > 0) {
            return res.status(400).json({ message: 'Email này đã tồn tại trong hệ thống!' });
        }

        // Băm mật khẩu nhận từ Android bằng thư viện bcrypt nhằm bảo mật thông tin
        const encryptedPassword = await bcrypt.hash(passwordHash, 10);
        
        // Chèn thông tin tài khoản mới vào bảng users dựa trên schema db.sql
        const [userResult] = await db.query(
            'INSERT INTO users (email, password_hash, full_name, target_goal) VALUES (?, ?, ?, ?)',
            [email, encryptedPassword, fullName, targetGoal || 'TOEIC 700']
        );
        const userId = userResult.insertId;

        // TỰ ĐỘNG KHỞI TẠO: Tạo bản ghi rỗng cho phần Cài đặt và Thống kê để tránh lỗi null dữ liệu
        await db.query('INSERT INTO user_settings (user_id) VALUES (?)', [userId]);
        await db.query('INSERT INTO user_statistics (user_id) VALUES (?)', [userId]);

        res.status(201).json({ message: 'Đăng ký tài khoản MinLish thành công!' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ==========================================
// 2. API ĐĂNG NHẬP HỆ THỐNG
// ==========================================
app.post('/api/auth/login', async (req, res) => {
    const { email, password } = req.body;

    if (!email || !password) {
        return res.status(400).json({ message: 'Vui lòng cung cấp đầy đủ email và mật khẩu!' });
    }

    try {
        // Tìm kiếm người dùng dựa trên email nhập vào
        const [users] = await db.query('SELECT * FROM users WHERE email = ?', [email]);
        if (users.length === 0) {
            return res.status(400).json({ message: 'Tài khoản hoặc mật khẩu không chính xác!' });
        }

        const user = users[0];
        
        // So khớp mật khẩu thuần người dùng nhập với chuỗi băm lưu giữ trong MySQL
        const isMatch = await bcrypt.compare(password, user.password_hash);
        if (!isMatch) {
            return res.status(400).json({ message: 'Tài khoản hoặc mật khẩu không chính xác!' });
        }

        // Tạo chuỗi mã Token JWT có giá trị sử dụng liên tục trong vòng 30 ngày
        const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: '30d' });
        
        res.json({ 
            token, 
            user: { 
                id: user.id, 
                full_name: user.full_name, 
                email: user.email 
            } 
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ==========================================
// MIDDLEWARE XÁC THỰC QUYỀN TRUY CẬP (JWT)
// ==========================================
const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    
    if (!token) {
        return res.status(401).json({ message: 'Quyền truy cập bị từ chối do thiếu Token xác thực!' });
    }

    jwt.verify(token, JWT_SECRET, (err, decodedUser) => {
        if (err) {
            return res.status(403).json({ message: 'Token của bạn đã hết hạn hoặc không hợp lệ!' });
        }
        req.user = decodedUser; // Lưu giữ thông tin định danh của user vào request để dùng cho các hàm sau
        next();
    });
};

// ==========================================
// 3. API TRANG CHỦ (LẤY DỮ LIỆU THỐNG KÊ DASHBOARD)
// ==========================================
app.get('/api/dashboard', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        
        // Câu truy vấn tổng hợp. Đã thay đổi alias bảng user_settings từ "set" thành "us" để tránh lỗi MySQL trùng từ khóa hệ thống.
        const [data] = await db.query(`
            SELECT u.full_name, u.target_goal, u.current_level,
                   s.current_streak, s.total_words_learned, s.accuracy_rate,
                   us.daily_new_words_goal
            FROM users u
            JOIN user_statistics s ON u.id = s.user_id
            JOIN user_settings us ON u.id = us.user_id
            WHERE u.id = ?
        `, [userId]);

        if (data.length === 0) {
            return res.status(404).json({ message: 'Không tìm thấy dữ liệu thống kê của người dùng này!' });
        }
        
        // Trả về kết quả JSON, cấu trúc khớp 100% với DashboardResponse data class của Kotlin
        res.json(data[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Khởi chạy ứng dụng Server lắng nghe ở cổng 3000
const PORT = 3000;
app.listen(PORT, () => console.log(`🚀 Hệ thống Backend MinLish đang hoạt động ổn định tại http://localhost:${PORT}`));