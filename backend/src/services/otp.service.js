const otpStore = new Map();

function generateOtp() {
    return Math.floor(100000 + Math.random() * 900000).toString();
}

function saveOtp(email, code) {
    otpStore.set(email, {
        code,
        expiresAt: Date.now() + 10 * 60 * 1000
    });
}

function getOtp(email) {
    return otpStore.get(email);
}

function deleteOtp(email) {
    otpStore.delete(email);
}

module.exports = {
    generateOtp,
    saveOtp,
    getOtp,
    deleteOtp
};
