const env = require('../config/env');
const notificationService = require('../services/notification.service');

function startDailyEmailReminderJob() {
    if (!env.notifications.enableEmailReminders) {
        console.log('Daily email reminders are disabled. Set ENABLE_EMAIL_REMINDERS=true to enable them.');
        return;
    }

    let lastRunKey = null;

    setInterval(async () => {
        const now = new Date();
        const isReminderTime =
            now.getHours() === env.notifications.dailyReminderHour &&
            now.getMinutes() === env.notifications.dailyReminderMinute;

        if (!isReminderTime) return;

        const runKey = now.toISOString().slice(0, 10);
        if (runKey === lastRunKey) return;

        lastRunKey = runKey;
        try {
            const result = await notificationService.sendDailyReminderEmails();
            console.log(`Daily reminder emails sent. sent=${result.sent}, failed=${result.failed}`);
        } catch (err) {
            console.error('Daily reminder email job failed:', err.message);
        }
    }, 60 * 1000);
}

module.exports = {
    startDailyEmailReminderJob
};
