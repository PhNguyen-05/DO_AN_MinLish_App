const transporter = require('../config/mail');
const env = require('../config/env');
const { createHttpError } = require('../utils/httpError');

async function sendPasswordResetOtp(email, otp) {
    if (!transporter) {
        throw createHttpError(500, 'SMTP chưa được cấu hình trên server. Vui lòng thiết lập SMTP_USER và SMTP_PASS.');
    }

    await transporter.sendMail({
        from: `"MinLish" <${env.smtp.from}>`,
        to: email,
        subject: 'MinLish - Mã OTP đặt lại mật khẩu',
        text: `Mã OTP của bạn là ${otp}. Mã có hiệu lực trong 10 phút.`
    });
}

async function sendStudyReminderEmail(email, payload) {
    if (!transporter) {
        throw createHttpError(500, 'SMTP chưa được cấu hình trên server. Vui lòng thiết lập SMTP_USER và SMTP_PASS.');
    }

    const name = payload.full_name || 'bạn';
    const newWordsLeft = Number(payload.new_words_available || 0);
    const dueReviewCount = Number(payload.due_review_count || 0);
    const learnedToday = Number(payload.words_learned_today || 0);
    const dailyGoal = Number(payload.daily_new_words_goal || 0);
    const reviewedToday = Number(payload.words_reviewed_today || 0);
    const reviewGoal = Number(payload.daily_review_goal || 0);

    await transporter.sendMail({
        from: `"MinLish" <${env.smtp.from}>`,
        to: email,
        subject: 'MinLish - Nhắc học từ vựng hôm nay',
        text: [
            `Chào ${name},`,
            '',
            'Đã đến lúc quay lại MinLish để giữ nhịp học hôm nay.',
            `Từ mới hôm nay: ${learnedToday}/${dailyGoal}.`,
            `Thẻ ôn tập hôm nay: ${reviewedToday}/${reviewGoal}.`,
            `Hiện còn ${newWordsLeft} từ mới có thể học và ${dueReviewCount} thẻ đến hạn ôn.`,
            '',
            'Mở MinLish để tiếp tục học nhé.'
        ].join('\n')
    });
}

module.exports = {
    sendPasswordResetOtp,
    sendStudyReminderEmail
};
