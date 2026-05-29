const asyncHandler = require('../utils/asyncHandler');
const notificationService = require('../services/notification.service');

const getSummary = asyncHandler(async (req, res) => {
    const summary = await notificationService.getNotificationSummary(req.user.id);
    res.json(summary);
});

const sendStudyReminderEmail = asyncHandler(async (req, res) => {
    const result = await notificationService.sendStudyReminderEmail(req.user.id);
    res.json(result);
});

module.exports = {
    getSummary,
    sendStudyReminderEmail
};
