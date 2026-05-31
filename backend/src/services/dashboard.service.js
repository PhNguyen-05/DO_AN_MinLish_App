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

async function getProgressByUserId(userId) {
    const [activityRows] = await db.query(`
        SELECT DATE_FORMAT(study_date, '%Y-%m-%d') AS date,
               words_learned,
               words_reviewed
        FROM daily_progress
        WHERE user_id = ?
          AND study_date >= DATE_SUB(CURDATE(), INTERVAL 13 DAY)
        ORDER BY study_date ASC
    `, [userId]);

    const activityByDate = new Map(activityRows.map(row => [
        row.date,
        {
            date: row.date,
            words_learned: Number(row.words_learned || 0),
            words_reviewed: Number(row.words_reviewed || 0)
        }
    ]));

    const daily_activity = [];
    for (let offset = 13; offset >= 0; offset -= 1) {
        const date = new Date();
        date.setDate(date.getDate() - offset);
        const key = toDateKey(date);
        daily_activity.push(activityByDate.get(key) || {
            date: key,
            words_learned: 0,
            words_reviewed: 0
        });
    }

    const [[retention]] = await db.query(`
        SELECT AVG(CASE WHEN quality >= 2 THEN 1 ELSE 0 END) AS rate
        FROM learning_logs
        WHERE user_id = ?
          AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
    `, [userId]);

    const [[stats]] = await db.query(`
        SELECT COALESCE(total_words_learned, 0) AS total_words_learned,
               COALESCE(accuracy_rate, 0) AS accuracy_rate
        FROM user_statistics
        WHERE user_id = ?
    `, [userId]);

    const totalWordsLearned = Number(stats?.total_words_learned || 0);
    const accuracyRate = Number(stats?.accuracy_rate || 0);
    const retentionRate = Number(retention?.rate || 0);
    const estimatedLevel = estimateLevel(totalWordsLearned, accuracyRate, retentionRate);

    return {
        daily_activity,
        retention_rate: retentionRate,
        estimated_level: estimatedLevel,
        level_reason: buildLevelReason(totalWordsLearned, accuracyRate, retentionRate)
    };
}

function estimateLevel(totalWordsLearned, accuracyRate, retentionRate) {
    if (totalWordsLearned >= 500 && accuracyRate >= 0.75 && retentionRate >= 0.70) {
        return 'Advanced';
    }

    if (totalWordsLearned >= 150 && accuracyRate >= 0.60 && retentionRate >= 0.55) {
        return 'Intermediate';
    }

    return 'Beginner';
}

function buildLevelReason(totalWordsLearned, accuracyRate, retentionRate) {
    const accuracyPercent = Math.round(accuracyRate * 100);
    const retentionPercent = Math.round(retentionRate * 100);
    return `${totalWordsLearned} từ đã học, đúng ${accuracyPercent}%, ghi nhớ ${retentionPercent}% trong 30 ngày gần nhất.`;
}

function toDateKey(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

module.exports = {
    getDashboardByUserId,
    getProgressByUserId
};
