const env = require('../config/env');
const notificationService = require('../services/notification.service');

const CHECK_INTERVAL_MS = 30 * 1000;

function getReminderClock(now = new Date()) {
    const parts = new Intl.DateTimeFormat('en-CA', {
        timeZone: env.notifications.timeZone,
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hourCycle: 'h23'
    }).formatToParts(now);
    const values = Object.fromEntries(parts.map(({ type, value }) => [type, value]));
    const date = `${values.year}-${values.month}-${values.day}`;
    const time = `${values.hour}:${values.minute}`;

    return {
        date,
        time,
        runKey: `${date} ${time}`
    };
}

function startDailyEmailReminderJob() {
    if (!env.notifications.enableEmailReminders) {
        console.log('Daily email reminders are disabled. Set ENABLE_EMAIL_REMINDERS=true to enable them.');
        return;
    }

    let lastRunKey = null;
    let running = false;

    const run = async () => {
        const clock = getReminderClock();
        if (running || clock.runKey === lastRunKey) return;

        running = true;
        lastRunKey = clock.runKey;
        try {
            const result = await notificationService.sendDueReminderEmails(clock.time);
            if (result.due > 0 || result.failed > 0) {
                console.log(
                    `Reminder emails processed for ${clock.time} (${env.notifications.timeZone}). ` +
                    `due=${result.due}, sent=${result.sent}, skipped=${result.skipped}, failed=${result.failed}`
                );
            }
        } catch (err) {
            lastRunKey = null;
            console.error('Daily reminder email job failed:', err.message);
        } finally {
            running = false;
        }
    };

    void run();
    return setInterval(() => void run(), CHECK_INTERVAL_MS);
}

module.exports = {
    getReminderClock,
    startDailyEmailReminderJob
};
