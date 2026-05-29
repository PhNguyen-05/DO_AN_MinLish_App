const db = require('../config/db');
const { createHttpError } = require('../utils/httpError');

async function getDashboardByUserId(userId) {
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
        throw createHttpError(404, 'Không tìm thấy dữ liệu thống kê của người dùng này!');
    }

    return data[0];
}

module.exports = {
    getDashboardByUserId
};
