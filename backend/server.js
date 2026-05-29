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

// MÃ£ khÃ³a bÃ­ máº­t Ä‘á»ƒ mÃ£ hÃ³a vÃ  giáº£i mÃ£ JWT Token
const JWT_SECRET = "minlish_super_secret_key_2026";

// Cáº¥u hÃ¬nh káº¿t ná»‘i MySQL Database káº¿t há»£p Connection Pool tá»‘i Æ°u hiá»‡u nÄƒng
const db = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '123456', // Thay báº±ng máº­t kháº©u MySQL chuáº©n trÃªn mÃ¡y cá»§a báº¡n
    database: 'minlish_db',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

// ==========================================
// 1. API ÄÄ‚NG KÃ TÃ€I KHOáº¢N Má»šI
// ==========================================
app.post('/api/auth/register', async (req, res) => {
    // Há»©ng chÃ­nh xÃ¡c cÃ¡c biáº¿n dáº¡ng CamelCase gá»­i lÃªn tá»« táº§ng Frontend Kotlin
    const { email, passwordHash, fullName, targetGoal, avatarBase64, avatarMimeType } = req.body;

    if (!email || !passwordHash || !fullName) {
        return res.status(400).json({ message: 'Vui lÃ²ng nháº­p Ä‘áº§y Ä‘á»§ cÃ¡c trÆ°á»ng thÃ´ng tin báº¯t buá»™c!' });
    }

    try {
        // Kiá»ƒm tra xem Email Ä‘Ã£ cÃ³ ai Ä‘Äƒng kÃ½ trÆ°á»›c Ä‘Ã³ chÆ°a
        const [existingUsers] = await db.query('SELECT id FROM users WHERE email = ?', [email]);
        if (existingUsers.length > 0) {
            return res.status(400).json({ message: 'Email nÃ y Ä‘Ã£ tá»“n táº¡i trong há»‡ thá»‘ng!' });
        }

        // BÄƒm máº­t kháº©u nháº­n tá»« Android báº±ng thÆ° viá»‡n bcrypt nháº±m báº£o máº­t thÃ´ng tin
        const encryptedPassword = await bcrypt.hash(passwordHash, 10);
        const avatarUrl = saveAvatarFromBase64(avatarBase64, avatarMimeType);
        
        // ChÃ¨n thÃ´ng tin tÃ i khoáº£n má»›i vÃ o báº£ng users dá»±a trÃªn schema db.sql
        const [userResult] = await db.query(
            'INSERT INTO users (email, password_hash, full_name, target_goal, avatar_url) VALUES (?, ?, ?, ?, ?)',
            [email, encryptedPassword, fullName, targetGoal || 'TOEIC 700', avatarUrl]
        );
        const userId = userResult.insertId;

        // Tá»° Äá»˜NG KHá»žI Táº O: Táº¡o báº£n ghi rá»—ng cho pháº§n CÃ i Ä‘áº·t vÃ  Thá»‘ng kÃª Ä‘á»ƒ trÃ¡nh lá»—i null dá»¯ liá»‡u
        await db.query('INSERT INTO user_settings (user_id) VALUES (?)', [userId]);
        await db.query('INSERT INTO user_statistics (user_id) VALUES (?)', [userId]);

        res.status(201).json({ message: 'ÄÄƒng kÃ½ tÃ i khoáº£n MinLish thÃ nh cÃ´ng!' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ==========================================
// 2. API ÄÄ‚NG NHáº¬P Há»† THá»NG
// ==========================================
app.post('/api/auth/login', async (req, res) => {
    const { email, password } = req.body;

    if (!email || !password) {
        return res.status(400).json({ message: 'Vui lÃ²ng cung cáº¥p Ä‘áº§y Ä‘á»§ email vÃ  máº­t kháº©u!' });
    }

    try {
        // TÃ¬m kiáº¿m ngÆ°á»i dÃ¹ng dá»±a trÃªn email nháº­p vÃ o
        const [users] = await db.query('SELECT * FROM users WHERE email = ?', [email]);
        if (users.length === 0) {
            return res.status(400).json({ message: 'TÃ i khoáº£n hoáº·c máº­t kháº©u khÃ´ng chÃ­nh xÃ¡c!' });
        }

        const user = users[0];
        
        // So khá»›p máº­t kháº©u thuáº§n ngÆ°á»i dÃ¹ng nháº­p vá»›i chuá»—i bÄƒm lÆ°u giá»¯ trong MySQL
        const isMatch = await bcrypt.compare(password, user.password_hash);
        if (!isMatch) {
            return res.status(400).json({ message: 'TÃ i khoáº£n hoáº·c máº­t kháº©u khÃ´ng chÃ­nh xÃ¡c!' });
        }

        // Táº¡o chuá»—i mÃ£ Token JWT cÃ³ giÃ¡ trá»‹ sá»­ dá»¥ng liÃªn tá»¥c trong vÃ²ng 30 ngÃ y
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
// MIDDLEWARE XÃC THá»°C QUYá»€N TRUY Cáº¬P (JWT)
// ==========================================
const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    
    if (!token) {
        return res.status(401).json({ message: 'Quyá»n truy cáº­p bá»‹ tá»« chá»‘i do thiáº¿u Token xÃ¡c thá»±c!' });
    }

    jwt.verify(token, JWT_SECRET, (err, decodedUser) => {
        if (err) {
            return res.status(403).json({ message: 'Token cá»§a báº¡n Ä‘Ã£ háº¿t háº¡n hoáº·c khÃ´ng há»£p lá»‡!' });
        }
        req.user = decodedUser; // LÆ°u giá»¯ thÃ´ng tin Ä‘á»‹nh danh cá»§a user vÃ o request Ä‘á»ƒ dÃ¹ng cho cÃ¡c hÃ m sau
        next();
    });
};

// ==========================================
// 3. API TRANG CHá»¦ (Láº¤Y Dá»® LIá»†U THá»NG KÃŠ DASHBOARD)
// ==========================================
app.get('/api/dashboard', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        
        // CÃ¢u truy váº¥n tá»•ng há»£p. ÄÃ£ thay Ä‘á»•i alias báº£ng user_settings tá»« "set" thÃ nh "us" Ä‘á»ƒ trÃ¡nh lá»—i MySQL trÃ¹ng tá»« khÃ³a há»‡ thá»‘ng.
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
            return res.status(404).json({ message: 'KhÃ´ng tÃ¬m tháº¥y dá»¯ liá»‡u thá»‘ng kÃª cá»§a ngÆ°á»i dÃ¹ng nÃ y!' });
        }
        
        // Tráº£ vá» káº¿t quáº£ JSON, cáº¥u trÃºc khá»›p 100% vá»›i DashboardResponse data class cá»§a Kotlin
        res.json(data[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ==========================================
// 4. QUÃŠN Máº¬T KHáº¨U - Gá»¬I OTP QUA EMAIL (Gmail)
// ==========================================
app.post('/api/auth/forgot-password', async (req, res) => {
    const { email } = req.body;
    if (!email) return res.status(400).json({ message: 'Vui lÃ²ng cung cáº¥p email.' });

    try {
        const [users] = await db.query('SELECT id, email FROM users WHERE email = ?', [email]);
        if (users.length === 0) return res.status(400).json({ message: 'KhÃ´ng tÃ¬m tháº¥y tÃ i khoáº£n vá»›i email nÃ y.' });

        if (!transporter) return res.status(500).json({ message: 'SMTP chÆ°a Ä‘Æ°á»£c cáº¥u hÃ¬nh trÃªn server. Vui lÃ²ng thiáº¿t láº­p SMTP_USER vÃ  SMTP_PASS.' });

        // Táº¡o mÃ£ OTP 6 chá»¯ sá»‘
        const otp = Math.floor(100000 + Math.random() * 900000).toString();
        const expiresAt = Date.now() + 10 * 60 * 1000; // 10 phÃºt

        // Gá»­i email chá»©a mÃ£ OTP
        const fromAddr = process.env.SMTP_FROM || smtpUser;
        await transporter.sendMail({
            from: fromAddr,
            to: email,
            subject: 'MinLish - MÃ£ OTP Ä‘áº·t láº¡i máº­t kháº©u',
            text: `MÃ£ OTP cá»§a báº¡n lÃ  ${otp}. MÃ£ cÃ³ hiá»‡u lá»±c trong 10 phÃºt.`
        });

        // LÆ°u OTP vÃ o bá»™ nhá»› táº¡m thá»i
        otpStore.set(email, { code: otp, expiresAt });

        res.json({ message: 'MÃ£ OTP Ä‘Ã£ Ä‘Æ°á»£c gá»­i tá»›i email cá»§a báº¡n. Vui lÃ²ng kiá»ƒm tra há»™p thÆ°.' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ==========================================
// 5. RESET Máº¬T KHáº¨U Vá»šI OTP
// ==========================================
app.post('/api/auth/reset-password', async (req, res) => {
    const { email, otp, newPassword } = req.body;
    if (!email || !otp || !newPassword) return res.status(400).json({ message: 'Vui lÃ²ng cung cáº¥p email, mÃ£ OTP vÃ  máº­t kháº©u má»›i.' });

    try {
        const record = otpStore.get(email);
        if (!record) return res.status(400).json({ message: 'KhÃ´ng cÃ³ mÃ£ OTP nÃ o Ä‘Æ°á»£c yÃªu cáº§u cho email nÃ y.' });
        if (record.code !== otp) return res.status(400).json({ message: 'MÃ£ OTP khÃ´ng há»£p lá»‡.' });
        if (record.expiresAt < Date.now()) {
            otpStore.delete(email);
            return res.status(400).json({ message: 'MÃ£ OTP Ä‘Ã£ háº¿t háº¡n.' });
        }

        // Hash máº­t kháº©u má»›i vÃ  cáº­p nháº­t vÃ o database
        const hashed = await bcrypt.hash(newPassword, 10);
        await db.query('UPDATE users SET password_hash = ? WHERE email = ?', [hashed, email]);

        // XÃ³a mÃ£ OTP sau khi sá»­ dá»¥ng
        otpStore.delete(email);

        res.json({ message: 'Máº­t kháº©u Ä‘Ã£ Ä‘Æ°á»£c Ä‘áº·t láº¡i thÃ nh cÃ´ng.' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ==========================================
// 6. Há»’ SÆ  NGÆ¯á»œI DÃ™NG - Láº¤Y VÃ€ Cáº¬P NHáº¬T
//    TrÆ°á»ng: full_name, target_goal, current_level
// ==========================================
app.get('/api/user/profile', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.id;
        const [rows] = await db.query('SELECT id, email, full_name, target_goal, current_level, avatar_url FROM users WHERE id = ?', [userId]);
        if (rows.length === 0) return res.status(404).json({ message: 'NgÆ°á»i dÃ¹ng khÃ´ng tá»“n táº¡i.' });
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

        if (fields.length === 0) return res.status(400).json({ message: 'KhÃ´ng cÃ³ dá»¯ liá»‡u Ä‘á»ƒ cáº­p nháº­t.' });

        values.push(userId);
        await db.query(`UPDATE users SET ${fields.join(', ')} WHERE id = ?`, values);

        if (oldAvatarUrl && oldAvatarUrl !== newAvatarUrl) {
            deleteAvatarFile(oldAvatarUrl);
        }

        // Tráº£ vá» há»“ sÆ¡ Ä‘Ã£ cáº­p nháº­t
        const [rows] = await db.query('SELECT id, email, full_name, target_goal, current_level, avatar_url FROM users WHERE id = ?', [userId]);
        res.json(rows[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Khởi chạy ứng dụng Server lắng nghe ở cổng 3000
const PORT = 3000;
app.listen(PORT, () => console.log(`🚀 Hệ thống Backend MinLish đang hoạt động ổn định tại http://localhost:${PORT}`));
