const userService = require('../services/user.service');
const asyncHandler = require('../utils/asyncHandler');

const getProfile = asyncHandler(async (req, res) => {
    const profile = await userService.getProfileByUserId(req.user.id);
    res.json(profile);
});

const updateProfile = asyncHandler(async (req, res) => {
    const profile = await userService.updateProfile(req.user.id, req.body);
    res.json(profile);
});

const getSettings = asyncHandler(async (req, res) => {
    const settings = await userService.getSettingsByUserId(req.user.id);
    res.json(settings);
});

const updateSettings = asyncHandler(async (req, res) => {
    const settings = await userService.updateSettings(req.user.id, req.body);
    res.json(settings);
});

module.exports = {
    getProfile,
    updateProfile,
    getSettings,
    updateSettings
};
