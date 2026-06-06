const express = require('express');
const learningController = require('../controllers/learning.controller');
const authenticateToken = require('../middlewares/auth.middleware');

const router = express.Router();

router.get('/plan', authenticateToken, learningController.getDailyPlan);
router.get('/decks', authenticateToken, learningController.getDeckSummaries);
router.get('/session', authenticateToken, learningController.getLearningSession);
router.get('/practice/decks', authenticateToken, learningController.getPracticeDeckSummaries);
router.get('/practice/cards', authenticateToken, learningController.getPracticeCards);
router.post('/review', authenticateToken, learningController.reviewCard);

module.exports = router;
