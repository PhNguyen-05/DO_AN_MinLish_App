const path = require('path');
const dotenv = require('dotenv');

const rootDir = path.join(__dirname, '..', '..');

// Load backend/.env even when node is run from another folder.
dotenv.config({ path: path.join(rootDir, '.env'), quiet: true });

const smtpUser = process.env.SMTP_USER;
const smtpPass = process.env.SMTP_PASS;
const defaultGoogleClientId = '1018329968245-ak7cqp2ire3arj9ef7o4f85jqia0ci31.apps.googleusercontent.com';
const defaultKeepAliveUrl = 'https://do-an-minlish-app.onrender.com/health/db';

function numberEnv(name, fallback) {
    const value = process.env[name];
    if (!value) {
        return fallback;
    }

    const parsed = Number.parseInt(value, 10);
    return Number.isNaN(parsed) ? fallback : parsed;
}

module.exports = {
    rootDir,
    port: process.env.PORT || 3000,
    jwtSecret: process.env.JWT_SECRET || 'minlish_super_secret_key_2026',
    googleClientId: process.env.GOOGLE_CLIENT_ID || defaultGoogleClientId,
    db: {
        host: process.env.DB_HOST || process.env.MYSQLHOST || 'localhost',
        port: numberEnv('DB_PORT', numberEnv('MYSQLPORT', 3306)),
        user: process.env.DB_USER || process.env.MYSQLUSER || 'root',
        password: process.env.DB_PASSWORD || process.env.MYSQLPASSWORD || '123456',
        database: process.env.DB_NAME || process.env.MYSQLDATABASE || 'minlish_db'
    },
    smtp: {
        user: smtpUser,
        pass: smtpPass,
        host: process.env.SMTP_HOST || 'smtp.gmail.com',
        port: process.env.SMTP_PORT ? parseInt(process.env.SMTP_PORT, 10) : 587,
        secure: process.env.SMTP_SECURE === 'true',
        from: process.env.SMTP_FROM || smtpUser,
        family: numberEnv('SMTP_FAMILY', 4)
    },
    notifications: {
        enableEmailReminders: process.env.ENABLE_EMAIL_REMINDERS === 'true',
        timeZone: process.env.REMINDER_TIME_ZONE || Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
    },
    keepAlive: {
        enabled: process.env.ENABLE_KEEP_ALIVE
            ? process.env.ENABLE_KEEP_ALIVE === 'true'
            : process.env.NODE_ENV === 'production',
        url: process.env.KEEP_ALIVE_URL || defaultKeepAliveUrl,
        intervalMinutes: numberEnv('KEEP_ALIVE_INTERVAL_MINUTES', 10)
    }
};
