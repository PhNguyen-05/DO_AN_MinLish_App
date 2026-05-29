const express = require('express');
const userController = require('../controllers/user.controller');
const authenticateToken = require('../middlewares/auth.middleware');

const router = express.Router();

router.get('/profile', authenticateToken, userController.getProfile);
router.put('/profile', authenticateToken, userController.updateProfile);
router.get('/settings', authenticateToken, userController.getSettings);
router.put('/settings', authenticateToken, userController.updateSettings);

module.exports = router;
