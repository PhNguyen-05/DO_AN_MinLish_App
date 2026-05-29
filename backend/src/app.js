const cors = require('cors');
const express = require('express');
const path = require('path');
const env = require('./config/env');
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
