const db = require('../config/db');
const { deleteAvatarFile, saveAvatarFromBase64 } = require('../utils/avatarStorage');
const { createHttpError } = require('../utils/httpError');

async function getProfileByUserId(userId) {
    const [rows] = await db.query(
        'SELECT id, email, full_name, target_goal, current_level, avatar_url FROM users WHERE id = ?',
        [userId]
    );

    if (rows.length === 0) {
        throw createHttpError(404, 'Người dùng không tồn tại.');
    }

    return rows[0];
}

async function updateProfile(userId, payload) {
    const { fullName, targetGoal, currentLevel, avatarBase64, avatarMimeType, removeAvatar } = payload;
    const [existingRows] = await db.query('SELECT avatar_url FROM users WHERE id = ?', [userId]);

    if (existingRows.length === 0) {
        throw createHttpError(404, 'Người dùng không tồn tại.');
    }

    const oldAvatarUrl = existingRows[0]?.avatar_url;
    let newAvatarUrl = oldAvatarUrl;

    const fields = [];
    const values = [];
    if (fullName !== undefined) {
        fields.push('full_name = ?');
        values.push(fullName);
    }
    if (targetGoal !== undefined) {
        fields.push('target_goal = ?');
        values.push(targetGoal);
    }
    if (currentLevel !== undefined) {
        fields.push('current_level = ?');
        values.push(currentLevel);
    }
    if (avatarBase64) {
        newAvatarUrl = saveAvatarFromBase64(avatarBase64, avatarMimeType);
        fields.push('avatar_url = ?');
        values.push(newAvatarUrl);
    } else if (removeAvatar === true) {
        newAvatarUrl = null;
        fields.push('avatar_url = ?');
        values.push(null);
    }

    if (fields.length === 0) {
        throw createHttpError(400, 'Không có dữ liệu để cập nhật.');
    }

    values.push(userId);
    await db.query(`UPDATE users SET ${fields.join(', ')} WHERE id = ?`, values);

    if (oldAvatarUrl && oldAvatarUrl !== newAvatarUrl) {
        deleteAvatarFile(oldAvatarUrl);
    }

    return getProfileByUserId(userId);
}

async function getSettingsByUserId(userId) {
    const [rows] = await db.query(
        'SELECT theme, daily_reminder_time, notifications_enabled, email_notifications_enabled, daily_new_words_goal, daily_review_goal FROM user_settings WHERE user_id = ?',
        [userId]
    );

    if (rows.length === 0) {
        await db.query('INSERT INTO user_settings (user_id) VALUES (?)', [userId]);
        const [newRows] = await db.query(
            'SELECT theme, daily_reminder_time, notifications_enabled, email_notifications_enabled, daily_new_words_goal, daily_review_goal FROM user_settings WHERE user_id = ?',
            [userId]
        );
        return newRows[0];
    }

    return rows[0];
}

async function updateSettings(userId, payload) {
    const { theme, dailyReminderTime, notificationsEnabled, emailNotificationsEnabled, dailyNewWordsGoal, dailyReviewGoal } = payload;
    
    const fields = [];
    const values = [];

    if (theme !== undefined) {
        fields.push('theme = ?');
        values.push(theme);
    }
    if (dailyReminderTime !== undefined) {
        fields.push('daily_reminder_time = ?');
        values.push(dailyReminderTime);
    }
    if (notificationsEnabled !== undefined) {
        fields.push('notifications_enabled = ?');
        values.push(notificationsEnabled ? 1 : 0);
    }
    if (emailNotificationsEnabled !== undefined) {
        fields.push('email_notifications_enabled = ?');
        values.push(emailNotificationsEnabled ? 1 : 0);
    }
    if (dailyNewWordsGoal !== undefined) {
        fields.push('daily_new_words_goal = ?');
        values.push(dailyNewWordsGoal);
    }
    if (dailyReviewGoal !== undefined) {
        fields.push('daily_review_goal = ?');
        values.push(dailyReviewGoal);
    }

    if (fields.length === 0) {
        throw createHttpError(400, 'Không có cài đặt nào cần cập nhật.');
    }

    values.push(userId);
    await db.query(`UPDATE user_settings SET ${fields.join(', ')} WHERE user_id = ?`, values);

    return getSettingsByUserId(userId);
}

module.exports = {
    getProfileByUserId,
    updateProfile,
    getSettingsByUserId,
    updateSettings
};
