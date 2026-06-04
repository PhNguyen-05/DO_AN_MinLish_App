const bcrypt = require('bcryptjs');
const crypto = require('crypto');
const { OAuth2Client } = require('google-auth-library');
const jwt = require('jsonwebtoken');
const db = require('../config/db');
const env = require('../config/env');
const { saveAvatarFromBase64 } = require('../utils/avatarStorage');
const { createHttpError } = require('../utils/httpError');
const otpService = require('./otp.service');
const mailService = require('./mail.service');

const googleClient = new OAuth2Client();
const SPECIAL_PASSWORD_CHARACTERS = '!@#$%^&*()_+=[]{}|;:,.<>?/-';

function normalizeEmail(email) {
    return String(email || '').trim().toLowerCase();
}

function validatePassword(password) {
    const value = String(password || '');
    if (value.length < 8 ||
        !/[A-Z]/.test(value) ||
        !/[a-z]/.test(value) ||
        !/[0-9]/.test(value) ||
        ![...value].some(char => SPECIAL_PASSWORD_CHARACTERS.includes(char))) {
        throw createHttpError(
            400,
            'Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt.'
        );
    }
}

function createAuthResponse(user) {
    const token = jwt.sign({ id: user.id, email: user.email }, env.jwtSecret, { expiresIn: '30d' });

    return {
        token,
        user: {
            id: user.id,
            full_name: user.full_name,
            email: user.email,
            avatar_url: user.avatar_url
        }
    };
}

async function createDefaultUserRecords(userId) {
    await db.query('INSERT INTO user_settings (user_id) VALUES (?)', [userId]);
    await db.query('INSERT INTO user_statistics (user_id) VALUES (?)', [userId]);
}

async function registerUser(payload) {
    const { email, passwordHash, fullName, targetGoal, avatarBase64, avatarMimeType } = payload;
    const normalizedEmail = normalizeEmail(email);
    const normalizedFullName = String(fullName || '').trim();

    if (!normalizedEmail || !passwordHash || !normalizedFullName) {
        throw createHttpError(400, 'Vui lòng nhập đầy đủ các trường thông tin bắt buộc!');
    }

    validatePassword(passwordHash);

    const [existingUsers] = await db.query('SELECT id FROM users WHERE email = ?', [normalizedEmail]);
    if (existingUsers.length > 0) {
        throw createHttpError(400, 'Email này đã tồn tại trong hệ thống!');
    }

    const encryptedPassword = await bcrypt.hash(passwordHash, 10);
    const avatarUrl = saveAvatarFromBase64(avatarBase64, avatarMimeType);

    const [userResult] = await db.query(
        'INSERT INTO users (email, password_hash, full_name, target_goal, avatar_url) VALUES (?, ?, ?, ?, ?)',
        [normalizedEmail, encryptedPassword, normalizedFullName, targetGoal || 'TOEIC 700', avatarUrl]
    );
    const userId = userResult.insertId;

    await createDefaultUserRecords(userId);

    return { message: 'Đăng ký tài khoản MinLish thành công!' };
}

async function loginUser(payload) {
    const { email, password } = payload;
    const normalizedEmail = normalizeEmail(email);

    if (!normalizedEmail || !password) {
        throw createHttpError(400, 'Vui lòng cung cấp đầy đủ email và mật khẩu!');
    }

    const [users] = await db.query('SELECT * FROM users WHERE email = ?', [normalizedEmail]);
    if (users.length === 0) {
        throw createHttpError(400, 'Tài khoản hoặc mật khẩu không chính xác!');
    }

    const user = users[0];
    const isMatch = await bcrypt.compare(password, user.password_hash);
    if (!isMatch) {
        throw createHttpError(400, 'Tài khoản hoặc mật khẩu không chính xác!');
    }

    return createAuthResponse(user);
}

async function verifyGoogleIdToken(idToken) {
    if (!idToken) {
        throw createHttpError(400, 'Vui lòng gửi Google ID token.');
    }
    if (!env.googleClientId) {
        throw createHttpError(500, 'Backend chưa cấu hình GOOGLE_CLIENT_ID.');
    }

    let ticket;
    try {
        ticket = await googleClient.verifyIdToken({
            idToken,
            audience: env.googleClientId
        });
    } catch (error) {
        throw createHttpError(401, 'Google ID token không hợp lệ.');
    }

    const payload = ticket.getPayload();
    if (!payload || !payload.email) {
        throw createHttpError(401, 'Không lấy được email từ tài khoản Google.');
    }
    if (payload.email_verified !== true && payload.email_verified !== 'true') {
        throw createHttpError(401, 'Email Google chưa được xác thực.');
    }

    const email = normalizeEmail(payload.email);
    return {
        email,
        fullName: String(payload.name || email.split('@')[0]).trim(),
        picture: payload.picture || null
    };
}

async function loginWithGoogle(payload = {}) {
    const googleUser = await verifyGoogleIdToken(payload.idToken);
    const [users] = await db.query('SELECT * FROM users WHERE email = ?', [googleUser.email]);
    let user = users[0];

    if (!user) {
        const randomPassword = crypto.randomBytes(32).toString('hex');
        const encryptedPassword = await bcrypt.hash(randomPassword, 10);
        const [userResult] = await db.query(
            'INSERT INTO users (email, password_hash, full_name, target_goal, avatar_url) VALUES (?, ?, ?, ?, ?)',
            [googleUser.email, encryptedPassword, googleUser.fullName, 'TOEIC 700', googleUser.picture]
        );

        user = {
            id: userResult.insertId,
            email: googleUser.email,
            full_name: googleUser.fullName,
            avatar_url: googleUser.picture
        };
        await createDefaultUserRecords(user.id);
    } else if (!user.avatar_url && googleUser.picture) {
        await db.query('UPDATE users SET avatar_url = ? WHERE id = ?', [googleUser.picture, user.id]);
        user.avatar_url = googleUser.picture;
    }

    return createAuthResponse(user);
}

async function requestPasswordReset(payload) {
    const { email } = payload;
    const normalizedEmail = normalizeEmail(email);

    if (!normalizedEmail) {
        throw createHttpError(400, 'Vui lòng cung cấp email.');
    }

    const [users] = await db.query('SELECT id, email FROM users WHERE email = ?', [normalizedEmail]);
    if (users.length === 0) {
        throw createHttpError(400, 'Không tìm thấy tài khoản với email này.');
    }

    const otp = otpService.generateOtp();
    await mailService.sendPasswordResetOtp(normalizedEmail, otp);
    otpService.saveOtp(normalizedEmail, otp);

    return { message: 'Mã OTP đã được gửi tới email của bạn. Vui lòng kiểm tra hộp thư.' };
}

async function resetPassword(payload) {
    const { email, otp, newPassword } = payload;
    const normalizedEmail = normalizeEmail(email);

    if (!normalizedEmail || !otp || !newPassword) {
        throw createHttpError(400, 'Vui lòng cung cấp email, mã OTP và mật khẩu mới.');
    }

    validatePassword(newPassword);

    const record = otpService.getOtp(normalizedEmail);
    if (!record) {
        throw createHttpError(400, 'Không có mã OTP nào được yêu cầu cho email này.');
    }
    if (record.code !== otp) {
        throw createHttpError(400, 'Mã OTP không hợp lệ.');
    }
    if (record.expiresAt < Date.now()) {
        otpService.deleteOtp(normalizedEmail);
        throw createHttpError(400, 'Mã OTP đã hết hạn.');
    }

    const hashed = await bcrypt.hash(newPassword, 10);
    await db.query('UPDATE users SET password_hash = ? WHERE email = ?', [hashed, normalizedEmail]);
    otpService.deleteOtp(normalizedEmail);

    return { message: 'Mật khẩu đã được đặt lại thành công.' };
}

module.exports = {
    registerUser,
    loginUser,
    loginWithGoogle,
    requestPasswordReset,
    resetPassword
};
