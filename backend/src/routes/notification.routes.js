const express = require('express');
const notificationController = require('../controllers/notification.controller');
const authenticateToken = require('../middlewares/auth.middleware');

const router = express.Router();

router.get('/summary', authenticateToken, notificationController.getSummary);
router.post('/email/reminder', authenticateToken, notificationController.sendStudyReminderEmail);

module.exports = router;
