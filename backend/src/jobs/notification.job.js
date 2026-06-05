const env = require('../config/env');
const notificationService = require('../services/notification.service');

const CHECK_INTERVAL_MS = 30 * 1000;
const reminderJobState = {
    enabled: env.notifications.enableEmailReminders,
    timeZone: env.notifications.timeZone,
    checkIntervalSeconds: CHECK_INTERVAL_MS / 1000,
    running: false,
    lastRun: null,
    lastResult: null,
    lastError: null
};

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
        reminderJobState.enabled = false;
        console.log('Daily email reminders are disabled. Set ENABLE_EMAIL_REMINDERS=true to enable them.');
        return;
    }

    reminderJobState.enabled = true;
    let lastRunKey = null;
    let running = false;

    const run = async () => {
        const clock = getReminderClock();
        if (running || clock.runKey === lastRunKey) return;

        running = true;
        reminderJobState.running = true;
        lastRunKey = clock.runKey;
        try {
            const result = await notificationService.sendDueReminderEmails(clock.time);
            reminderJobState.lastRun = {
                date: clock.date,
                time: clock.time,
                runKey: clock.runKey,
                checkedAt: new Date().toISOString()
            };
            reminderJobState.lastResult = result;
            reminderJobState.lastError = null;
            if (result.due > 0 || result.failed > 0) {
                console.log(
                    `Reminder emails processed for ${clock.time} (${env.notifications.timeZone}). ` +
                    `due=${result.due}, sent=${result.sent}, skipped=${result.skipped}, failed=${result.failed}`
                );
            }
        } catch (err) {
            lastRunKey = null;
            reminderJobState.lastRun = {
                date: clock.date,
                time: clock.time,
                runKey: clock.runKey,
                checkedAt: new Date().toISOString()
            };
            reminderJobState.lastError = err.message;
            console.error('Daily reminder email job failed:', err.message);
        } finally {
            running = false;
            reminderJobState.running = false;
        }
    };

    void run();
    return setInterval(() => void run(), CHECK_INTERVAL_MS);
}

function getDailyEmailReminderJobState() {
    return { ...reminderJobState };
}

module.exports = {
    getReminderClock,
    startDailyEmailReminderJob,
    getDailyEmailReminderJobState
};
