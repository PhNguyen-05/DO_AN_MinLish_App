const express = require('express');
const learningController = require('../controllers/learning.controller');
const authenticateToken = require('../middlewares/auth.middleware');

const router = express.Router();

router.get('/plan', authenticateToken, learningController.getDailyPlan);
router.get('/decks', authenticateToken, learningController.getDeckSummaries);
router.get('/session', authenticateToken, learningController.getLearningSession);
router.post('/review', authenticateToken, learningController.reviewCard);

module.exports = router;
