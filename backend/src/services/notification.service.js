const db = require('../config/db');
const learningService = require('./learning.service');
const mailService = require('./mail.service');
const { createHttpError } = require('../utils/httpError');

async function getNotificationSummary(userId) {
    const [[user]] = await db.query(`
        SELECT id, email, full_name
        FROM users
        WHERE id = ?
    `, [userId]);

    if (!user) {
        throw createHttpError(404, 'Không tìm thấy người dùng.');
    }

    const plan = await learningService.getDailyPlan(userId);
    const dueReviewCount = Number(plan.due_review_count || 0);
    const newWordsAvailable = Number(plan.new_words_available || 0);

    return {
        email: user.email,
        full_name: user.full_name,
        ...plan,
        push_title: 'MinLish nhắc học',
        push_body: buildReminderBody(newWordsAvailable, dueReviewCount)
    };
}

async function sendStudyReminderEmail(userId) {
    const summary = await getNotificationSummary(userId);
    await mailService.sendStudyReminderEmail(summary.email, summary);

    return {
        message: `Đã gửi email nhắc học tới ${summary.email}.`
    };
}

async function sendDailyReminderEmails() {
    const [users] = await db.query(`
        SELECT u.id
        FROM users u
        JOIN user_settings us ON u.id = us.user_id
        WHERE u.email IS NOT NULL AND u.email <> ''
          AND us.email_notifications_enabled = 1
    `);

    let sent = 0;
    let failed = 0;

    for (const user of users) {
        try {
            const plan = await learningService.getDailyPlan(user.id);
            const dueReviewCount = Number(plan.due_review_count || 0);
            const wordsLearnedToday = Number(plan.words_learned_today || 0);
            const wordsReviewedToday = Number(plan.words_reviewed_today || 0);

            const needsReminder = dueReviewCount > 0 || (wordsLearnedToday === 0 && wordsReviewedToday === 0);

            if (needsReminder) {
                await sendStudyReminderEmail(user.id);
                sent += 1;
            }
        } catch (err) {
            failed += 1;
            console.error(`Failed to send reminder email for user ${user.id}:`, err.message);
        }
    }

    return { sent, failed };
}

function buildReminderBody(newWordsAvailable, dueReviewCount) {
    if (dueReviewCount > 0) {
        return `Bạn có ${dueReviewCount} thẻ cần ôn và ${newWordsAvailable} từ mới có thể học.`;
    }

    if (newWordsAvailable > 0) {
        return `Bạn còn ${newWordsAvailable} từ mới để giữ nhịp học hôm nay.`;
    }

    return 'Mở MinLish để kiểm tra tiến độ học hôm nay.';
}

module.exports = {
    getNotificationSummary,
    sendStudyReminderEmail,
    sendDailyReminderEmails
};
