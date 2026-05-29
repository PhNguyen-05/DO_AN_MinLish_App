const path = require('path');
const dotenv = require('dotenv');

const rootDir = path.join(__dirname, '..', '..');

// Load backend/.env even when node is run from another folder.
dotenv.config({ path: path.join(rootDir, '.env'), quiet: true });

const smtpUser = process.env.SMTP_USER;
const smtpPass = process.env.SMTP_PASS;

module.exports = {
    rootDir,
    port: process.env.PORT || 3000,
    jwtSecret: process.env.JWT_SECRET || 'minlish_super_secret_key_2026',
    db: {
        host: process.env.DB_HOST || 'localhost',
        user: process.env.DB_USER || 'root',
        password: process.env.DB_PASSWORD || '123456',
        database: process.env.DB_NAME || 'minlish_db'
    },
    smtp: {
        user: smtpUser,
        pass: smtpPass,
        host: process.env.SMTP_HOST || 'smtp.gmail.com',
        port: process.env.SMTP_PORT ? parseInt(process.env.SMTP_PORT, 10) : 587,
        secure: process.env.SMTP_SECURE === 'true',
        from: process.env.SMTP_FROM || smtpUser
    },
    notifications: {
        enableEmailReminders: process.env.ENABLE_EMAIL_REMINDERS === 'true',
        dailyReminderHour: process.env.REMINDER_EMAIL_HOUR ? parseInt(process.env.REMINDER_EMAIL_HOUR, 10) : 20,
        dailyReminderMinute: process.env.REMINDER_EMAIL_MINUTE ? parseInt(process.env.REMINDER_EMAIL_MINUTE, 10) : 0
    }
};
