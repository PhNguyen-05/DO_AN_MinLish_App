const asyncHandler = require('../utils/asyncHandler');
const learningService = require('../services/learning.service');

const getDailyPlan = asyncHandler(async (req, res) => {
    const plan = await learningService.getDailyPlan(req.user.id);
    res.json(plan);
});

const getLearningSession = asyncHandler(async (req, res) => {
    const session = await learningService.getLearningSession(req.user.id, req.query);
    res.json(session);
});

const getDeckSummaries = asyncHandler(async (req, res) => {
    const summaries = await learningService.getDeckSummaries(req.user.id);
    res.json(summaries);
});

const reviewCard = asyncHandler(async (req, res) => {
    const progress = await learningService.reviewCard(req.user.id, req.body);
    res.json(progress);
});

module.exports = {
    getDailyPlan,
    getLearningSession,
    getDeckSummaries,
    reviewCard
};
