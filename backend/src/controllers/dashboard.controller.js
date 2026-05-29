const dashboardService = require('../services/dashboard.service');
const asyncHandler = require('../utils/asyncHandler');

const getDashboard = asyncHandler(async (req, res) => {
    const dashboard = await dashboardService.getDashboardByUserId(req.user.id);
    res.json(dashboard);
});

module.exports = {
    getDashboard
};
