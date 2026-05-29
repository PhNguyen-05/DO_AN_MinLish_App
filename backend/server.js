const express = require('express');
const mysql = require('mysql2/promise');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const cors = require('cors');
const dotenv = require('dotenv');
const nodemailer = require('nodemailer');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');

// Load environment variables from backend/.env even when node is run from another folder.
dotenv.config({ path: path.join(__dirname, '.env') });

const app = express();
app.use(express.json({ limit: '2mb' }));
app.use(cors());

// In-memory OTP store: { email -> { code, expiresAt } }
const otpStore = new Map();

const uploadRoot = path.join(__dirname, 'uploads');
const avatarUploadDir = path.join(uploadRoot, 'avatars');
fs.mkdirSync(avatarUploadDir, { recursive: true });
app.use('/uploads', express.static(uploadRoot));
app.use('/static', express.static('public'));

function saveAvatarFromBase64(avatarBase64, avatarMimeType = 'image/jpeg') {
    if (!avatarBase64) return null;

    const normalizedBase64 = avatarBase64.includes(',')
        ? avatarBase64.split(',').pop()
        : avatarBase64;
    const buffer = Buffer.from(normalizedBase64, 'base64');

    if (!buffer.length) return null;
    if (buffer.length > 1024 * 1024) {
        throw new Error('Ảnh avatar quá lớn. Vui lòng chọn ảnh nhỏ hơn 1MB.');
    }

    const extensionByMimeType = {
        'image/png': 'png',
        'image/webp': 'webp',
        'image/jpeg': 'jpg',
        'image/jpg': 'jpg'
    };
    const extension = extensionByMimeType[avatarMimeType] || 'jpg';
    const fileName = `${Date.now()}-${crypto.randomBytes(8).toString('hex')}.${extension}`;
    fs.writeFileSync(path.join(avatarUploadDir, fileName), buffer);

    return `/uploads/avatars/${fileName}`;
}

function deleteAvatarFile(avatarUrl) {
    if (!avatarUrl || !avatarUrl.startsWith('/uploads/avatars/')) return;

    const filePath = path.join(__dirname, avatarUrl.replace(/^\//, ''));
    if (filePath.startsWith(avatarUploadDir) && fs.existsSync(filePath)) {
        fs.unlinkSync(filePath);
    }
}

// Nodemailer transporter (configured via env vars)
const smtpUser = process.env.SMTP_USER;
const smtpPass = process.env.SMTP_PASS;
const smtpHost = process.env.SMTP_HOST || 'smtp.gmail.com';
const smtpPort = process.env.SMTP_PORT ? parseInt(process.env.SMTP_PORT, 10) : 587;
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

const JWT_SECRET = process.env.JWT_SECRET || 'minlish_super_secret_key_2026';

const db = mysql.createPool({
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '123456',
    database: process.env.DB_NAME || 'minlish_db',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

// ==========================================
// 1. API ĐĂNG KÝ TÀI KHOẢN MỚI
// ==========================================
app.post('/api/auth/register', async (req, res) => {
    const { email, passwordHash, fullName, targetGoal, avatarBase64, avatarMimeType } = req.body;

    if (!email || !passwordHash || !fullName) {
        return res.status(400).json({ message: 'Vui lòng nhập đầy đủ các trường thông tin bắt buộc!' });
    }

    try {
        const [existingUsers] = await db.query('SELECT id FROM users WHERE email = ?', [email]);
        if (existingUsers.length > 0) {
            return res.status(400).json({ message: 'Email này đã tồn tại trong hệ thống!' });
        }

        const encryptedPassword = await bcrypt.hash(passwordHash, 10);
        const avatarUrl = saveAvatarFromBase64(avatarBase64, avatarMimeType);

        const [userResult] = await db.query(
            'INSERT INTO users (email, password_hash, full_name, target_goal, avatar_url) VALUES (?, ?, ?, ?, ?)',
            [email, encryptedPassword, fullName, targetGoal || 'TOEIC 700', avatarUrl]
        );
        const userId = userResult.insertId;

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
        const [users] = await db.query('SELECT * FROM users WHERE email = ?', [email]);
        if (users.length === 0) {
            return res.status(400).json({ message: 'Tài khoản hoặc mật khẩu không chính xác!' });
        }

        const user = users[0];
        const isMatch = await bcrypt.compare(password, user.password_hash);
        if (!isMatch) {
            return res.status(400).json({ message: 'Tài khoản hoặc mật khẩu không chính xác!' });
        }

        const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: '30d' });
        res.json({ 
            token, 
            user: { 
                id: user.id, 
                full_name: user.full_name, 
                email: user.email,
                avatar_url: user.avatar_url
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

        req.user = decodedUser;
        next();
    });
};

// ==========================================
// 3. API TRANG CHỦ (LẤY DỮ LIỆU THỐNG KÊ DASHBOARD)
// ==========================================
app.get('/api/dashboard', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        const [data] = await db.query(`
            SELECT u.full_name, u.target_goal, u.current_level, u.avatar_url,
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

        const otp = Math.floor(100000 + Math.random() * 900000).toString();
        const expiresAt = Date.now() + 10 * 60 * 1000;

        const fromAddr = process.env.SMTP_FROM || smtpUser;
        await transporter.sendMail({
            from: fromAddr,
            to: email,
            subject: 'MinLish - Mã OTP đặt lại mật khẩu',
            text: `Mã OTP của bạn là ${otp}. Mã có hiệu lực trong 10 phút.`
        });

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

        const hashed = await bcrypt.hash(newPassword, 10);
        await db.query('UPDATE users SET password_hash = ? WHERE email = ?', [hashed, email]);
        otpStore.delete(email);

        res.json({ message: 'Mật khẩu đã được đặt lại thành công.' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ==========================================
// 6. HỒ SƠ NGƯỜI DÙNG - LẤY VÀ CẬP NHẬT
// ==========================================
app.get('/api/user/profile', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        const [rows] = await db.query('SELECT id, email, full_name, target_goal, current_level, avatar_url FROM users WHERE id = ?', [userId]);
        if (rows.length === 0) return res.status(404).json({ message: 'Người dùng không tồn tại.' });
        res.json(rows[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.put('/api/user/profile', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        const { fullName, targetGoal, currentLevel, avatarBase64, avatarMimeType, removeAvatar } = req.body;
        const [existingRows] = await db.query('SELECT avatar_url FROM users WHERE id = ?', [userId]);
        const oldAvatarUrl = existingRows[0]?.avatar_url;
        let newAvatarUrl = oldAvatarUrl;

        const fields = [];
        const values = [];
        if (fullName !== undefined) { fields.push('full_name = ?'); values.push(fullName); }
        if (targetGoal !== undefined) { fields.push('target_goal = ?'); values.push(targetGoal); }
        if (currentLevel !== undefined) { fields.push('current_level = ?'); values.push(currentLevel); }
        if (avatarBase64) {
            newAvatarUrl = saveAvatarFromBase64(avatarBase64, avatarMimeType);
            fields.push('avatar_url = ?');
            values.push(newAvatarUrl);
        } else if (removeAvatar === true) {
            newAvatarUrl = null;
            fields.push('avatar_url = ?');
            values.push(null);
        }

        if (fields.length === 0) return res.status(400).json({ message: 'Không có dữ liệu để cập nhật.' });

        values.push(userId);
        await db.query(`UPDATE users SET ${fields.join(', ')} WHERE id = ?`, values);

        if (oldAvatarUrl && oldAvatarUrl !== newAvatarUrl) {
            deleteAvatarFile(oldAvatarUrl);
        }

        const [rows] = await db.query('SELECT id, email, full_name, target_goal, current_level, avatar_url FROM users WHERE id = ?', [userId]);
        res.json(rows[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`🚀 Hệ thống Backend MinLish đang hoạt động ổn định tại http://localhost:${PORT}`));
