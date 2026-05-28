const express = require('express');
const mysql = require('mysql2/promise');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const cors = require('cors');
const dotenv = require('dotenv');
const nodemailer = require('nodemailer');
const path = require('path');

// Load environment variables from backend/.env even when node is run from another folder.
dotenv.config({ path: path.join(__dirname, '.env') });

const app = express();
app.use(express.json());
app.use(cors());

// In-memory OTP store: { email -> { code, expiresAt } }
const otpStore = new Map();

// Nodemailer transporter (configured via env vars)
const smtpUser = process.env.SMTP_USER;
const smtpPass = process.env.SMTP_PASS;
const smtpHost = process.env.SMTP_HOST || 'smtp.gmail.com';
const smtpPort = process.env.SMTP_PORT ? parseInt(process.env.SMTP_PORT) : 587;
const smtpSecure = process.env.SMTP_SECURE === 'true' || false;

let transporter;
if (smtpUser && smtpPass) {
    transporter = nodemailer.createTransport({
        host: smtpHost,
        port: smtpPort,
        secure: smtpSecure,
        auth: { user: smtpUser, pass: smtpPass }
    });
} else {
    console.warn('SMTP credentials not provided. Forgot-password emails will fail until SMTP_USER and SMTP_PASS are set.');
}

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

// ==========================================
// 4. QUÊN MẬT KHẨU - GỬI OTP QUA EMAIL (Gmail)
// ==========================================
app.post('/api/auth/forgot-password', async (req, res) => {
    const { email } = req.body;
    if (!email) return res.status(400).json({ message: 'Vui lòng cung cấp email.' });

    try {
        const [users] = await db.query('SELECT id, email FROM users WHERE email = ?', [email]);
        if (users.length === 0) return res.status(400).json({ message: 'Không tìm thấy tài khoản với email này.' });

        if (!transporter) return res.status(500).json({ message: 'SMTP chưa được cấu hình trên server. Vui lòng thiết lập SMTP_USER và SMTP_PASS.' });

        // Tạo mã OTP 6 chữ số
        const otp = Math.floor(100000 + Math.random() * 900000).toString();
        const expiresAt = Date.now() + 10 * 60 * 1000; // 10 phút

        // Gửi email chứa mã OTP
        const fromAddr = process.env.SMTP_FROM || smtpUser;
        await transporter.sendMail({
            from: fromAddr,
            to: email,
            subject: 'MinLish - Mã OTP đặt lại mật khẩu',
            text: `Mã OTP của bạn là ${otp}. Mã có hiệu lực trong 10 phút.`
        });

        // Lưu OTP vào bộ nhớ tạm thời
        otpStore.set(email, { code: otp, expiresAt });

        res.json({ message: 'Mã OTP đã được gửi tới email của bạn. Vui lòng kiểm tra hộp thư.' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ==========================================
// 5. RESET MẬT KHẨU VỚI OTP
// ==========================================
app.post('/api/auth/reset-password', async (req, res) => {
    const { email, otp, newPassword } = req.body;
    if (!email || !otp || !newPassword) return res.status(400).json({ message: 'Vui lòng cung cấp email, mã OTP và mật khẩu mới.' });

    try {
        const record = otpStore.get(email);
        if (!record) return res.status(400).json({ message: 'Không có mã OTP nào được yêu cầu cho email này.' });
        if (record.code !== otp) return res.status(400).json({ message: 'Mã OTP không hợp lệ.' });
        if (record.expiresAt < Date.now()) {
            otpStore.delete(email);
            return res.status(400).json({ message: 'Mã OTP đã hết hạn.' });
        }

        // Hash mật khẩu mới và cập nhật vào database
        const hashed = await bcrypt.hash(newPassword, 10);
        await db.query('UPDATE users SET password_hash = ? WHERE email = ?', [hashed, email]);

        // Xóa mã OTP sau khi sử dụng
        otpStore.delete(email);

        res.json({ message: 'Mật khẩu đã được đặt lại thành công.' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ==========================================
// 6. HỒ SƠ NGƯỜI DÙNG - LẤY VÀ CẬP NHẬT
//    Trường: full_name, target_goal, current_level
// ==========================================
app.get('/api/user/profile', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        const [rows] = await db.query('SELECT id, email, full_name, target_goal, current_level FROM users WHERE id = ?', [userId]);
        if (rows.length === 0) return res.status(404).json({ message: 'Người dùng không tồn tại.' });
        res.json(rows[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.put('/api/user/profile', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        const { fullName, targetGoal, currentLevel } = req.body;

        const fields = [];
        const values = [];
        if (fullName !== undefined) { fields.push('full_name = ?'); values.push(fullName); }
        if (targetGoal !== undefined) { fields.push('target_goal = ?'); values.push(targetGoal); }
        if (currentLevel !== undefined) { fields.push('current_level = ?'); values.push(currentLevel); }

        if (fields.length === 0) return res.status(400).json({ message: 'Không có dữ liệu để cập nhật.' });

        values.push(userId);
        await db.query(`UPDATE users SET ${fields.join(', ')} WHERE id = ?`, values);

        // Trả về hồ sơ đã cập nhật
        const [rows] = await db.query('SELECT id, email, full_name, target_goal, current_level FROM users WHERE id = ?', [userId]);
        res.json(rows[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Khởi chạy ứng dụng Server lắng nghe ở cổng 3000
const PORT = 3000;
app.listen(PORT, () => console.log(`🚀 Hệ thống Backend MinLish đang hoạt động ổn định tại http://localhost:${PORT}`));
