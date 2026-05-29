const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('../config/db');
const env = require('../config/env');
const { saveAvatarFromBase64 } = require('../utils/avatarStorage');
const { createHttpError } = require('../utils/httpError');
const otpService = require('./otp.service');
const mailService = require('./mail.service');

async function registerUser(payload) {
    const { email, passwordHash, fullName, targetGoal, avatarBase64, avatarMimeType } = payload;

    if (!email || !passwordHash || !fullName) {
        throw createHttpError(400, 'Vui lòng nhập đầy đủ các trường thông tin bắt buộc!');
    }

    const [existingUsers] = await db.query('SELECT id FROM users WHERE email = ?', [email]);
    if (existingUsers.length > 0) {
        throw createHttpError(400, 'Email này đã tồn tại trong hệ thống!');
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

    return { message: 'Đăng ký tài khoản MinLish thành công!' };
}

async function loginUser(payload) {
    const { email, password } = payload;

    if (!email || !password) {
        throw createHttpError(400, 'Vui lòng cung cấp đầy đủ email và mật khẩu!');
    }

    const [users] = await db.query('SELECT * FROM users WHERE email = ?', [email]);
    if (users.length === 0) {
        throw createHttpError(400, 'Tài khoản hoặc mật khẩu không chính xác!');
    }

    const user = users[0];
    const isMatch = await bcrypt.compare(password, user.password_hash);
    if (!isMatch) {
        throw createHttpError(400, 'Tài khoản hoặc mật khẩu không chính xác!');
    }

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

async function requestPasswordReset(payload) {
    const { email } = payload;

    if (!email) {
        throw createHttpError(400, 'Vui lòng cung cấp email.');
    }

    const [users] = await db.query('SELECT id, email FROM users WHERE email = ?', [email]);
    if (users.length === 0) {
        throw createHttpError(400, 'Không tìm thấy tài khoản với email này.');
    }

    const otp = otpService.generateOtp();
    await mailService.sendPasswordResetOtp(email, otp);
    otpService.saveOtp(email, otp);

    return { message: 'Mã OTP đã được gửi tới email của bạn. Vui lòng kiểm tra hộp thư.' };
}

async function resetPassword(payload) {
    const { email, otp, newPassword } = payload;

    if (!email || !otp || !newPassword) {
        throw createHttpError(400, 'Vui lòng cung cấp email, mã OTP và mật khẩu mới.');
    }

    const record = otpService.getOtp(email);
    if (!record) {
        throw createHttpError(400, 'Không có mã OTP nào được yêu cầu cho email này.');
    }
    if (record.code !== otp) {
        throw createHttpError(400, 'Mã OTP không hợp lệ.');
    }
    if (record.expiresAt < Date.now()) {
        otpService.deleteOtp(email);
        throw createHttpError(400, 'Mã OTP đã hết hạn.');
    }

    const hashed = await bcrypt.hash(newPassword, 10);
    await db.query('UPDATE users SET password_hash = ? WHERE email = ?', [hashed, email]);
    otpService.deleteOtp(email);

    return { message: 'Mật khẩu đã được đặt lại thành công.' };
}

module.exports = {
    registerUser,
    loginUser,
    requestPasswordReset,
    resetPassword
};
