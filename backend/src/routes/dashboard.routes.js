const express = require('express');
const dashboardController = require('../controllers/dashboard.controller');
const authenticateToken = require('../middlewares/auth.middleware');

const router = express.Router();

router.get('/', authenticateToken, dashboardController.getDashboard);

module.exports = router;
