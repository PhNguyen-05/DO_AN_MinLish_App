const asyncHandler = require('../utils/asyncHandler');
const authService = require('../services/auth.service');

const register = asyncHandler(async (req, res) => {
    const result = await authService.registerUser(req.body);
    res.status(201).json(result);
});

const login = asyncHandler(async (req, res) => {
    const result = await authService.loginUser(req.body);
    res.json(result);
});

const forgotPassword = asyncHandler(async (req, res) => {
    const result = await authService.requestPasswordReset(req.body);
    res.json(result);
});

const resetPassword = asyncHandler(async (req, res) => {
    const result = await authService.resetPassword(req.body);
    res.json(result);
});

module.exports = {
    register,
    login,
    forgotPassword,
    resetPassword
};
