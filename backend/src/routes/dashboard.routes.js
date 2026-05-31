const express = require('express');
const dashboardController = require('../controllers/dashboard.controller');
const authenticateToken = require('../middlewares/auth.middleware');

const router = express.Router();

router.get('/progress', authenticateToken, dashboardController.getProgress);
router.get('/', authenticateToken, dashboardController.getDashboard);

module.exports = router;
