const dashboardService = require('../services/dashboard.service');
const asyncHandler = require('../utils/asyncHandler');

const getDashboard = asyncHandler(async (req, res) => {
    const dashboard = await dashboardService.getDashboardByUserId(req.user.id);
    res.json(dashboard);
});

const getProgress = asyncHandler(async (req, res) => {
    const progress = await dashboardService.getProgressByUserId(req.user.id);
    res.json(progress);
});

module.exports = {
    getDashboard,
    getProgress
};
