const cors = require('cors');
const express = require('express');
const path = require('path');
const db = require('./config/db');
const env = require('./config/env');
const mailTransporter = require('./config/mail');
const { uploadRoot } = require('./utils/avatarStorage');
const authRoutes = require('./routes/auth.routes');
const dashboardRoutes = require('./routes/dashboard.routes');
const learningRoutes = require('./routes/learning.routes');
const notificationRoutes = require('./routes/notification.routes');
const userRoutes = require('./routes/user.routes');
const { errorHandler, notFoundHandler } = require('./middlewares/error.middleware');

const app = express();

app.use(express.json({ limit: '2mb' }));
app.use(cors());

app.get('/health', (req, res) => {
    res.json({ status: 'ok' });
});

app.get('/health/db', async (req, res, next) => {
    try {
        await db.query('SELECT 1 AS ok');
        res.json({ status: 'ok', database: 'ok' });
    } catch (error) {
        next(error);
    }
});

app.get('/health/smtp', async (req, res, next) => {
    const smtpConfig = {
        host: env.smtp.host,
        port: env.smtp.port,
        secure: env.smtp.secure,
        family: env.smtp.family,
        userConfigured: Boolean(env.smtp.user),
        fromConfigured: Boolean(env.smtp.from)
    };

    if (!mailTransporter) {
        return res.status(500).json({ status: 'error', smtp: 'not_configured', config: smtpConfig });
    }

    try {
        await mailTransporter.verify();
        res.json({ status: 'ok', smtp: 'ok', config: smtpConfig });
    } catch (error) {
        res.status(502).json({ status: 'error', smtp: 'unreachable', message: error.message, config: smtpConfig });
    }
});

app.use('/uploads', express.static(uploadRoot));
app.use('/static', express.static(path.join(env.rootDir, 'public')));

app.use('/api/auth', authRoutes);
app.use('/api/dashboard', dashboardRoutes);
app.use('/api/learning', learningRoutes);
app.use('/api/notifications', notificationRoutes);
app.use('/api/user', userRoutes);

app.use(notFoundHandler);
app.use(errorHandler);

module.exports = app;
