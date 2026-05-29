const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const env = require('../config/env');

const uploadRoot = path.join(env.rootDir, 'uploads');
const avatarUploadDir = path.join(uploadRoot, 'avatars');

function ensureAvatarUploadDir() {
    fs.mkdirSync(avatarUploadDir, { recursive: true });
}

function saveAvatarFromBase64(avatarBase64, avatarMimeType = 'image/jpeg') {
    if (!avatarBase64) return null;

    ensureAvatarUploadDir();

    const normalizedBase64 = avatarBase64.includes(',')
        ? avatarBase64.split(',').pop()
        : avatarBase64;
    const buffer = Buffer.from(normalizedBase64, 'base64');

    if (!buffer.length) return null;
    if (buffer.length > 1024 * 1024) {
        throw new Error('Ảnh avatar quá lớn. Vui lòng chọn ảnh nhỏ hơn 1MB.');
    }

    const extensionByMimeType = {
        'image/png': 'png',
        'image/webp': 'webp',
        'image/jpeg': 'jpg',
        'image/jpg': 'jpg'
    };
    const extension = extensionByMimeType[avatarMimeType] || 'jpg';
    const fileName = `${Date.now()}-${crypto.randomBytes(8).toString('hex')}.${extension}`;
    fs.writeFileSync(path.join(avatarUploadDir, fileName), buffer);

    return `/uploads/avatars/${fileName}`;
}

function deleteAvatarFile(avatarUrl) {
    if (!avatarUrl || !avatarUrl.startsWith('/uploads/avatars/')) return;

    const filePath = path.resolve(env.rootDir, avatarUrl.replace(/^\//, ''));
    const avatarDir = path.resolve(avatarUploadDir);
    if (filePath.startsWith(`${avatarDir}${path.sep}`) && fs.existsSync(filePath)) {
        fs.unlinkSync(filePath);
    }
}

ensureAvatarUploadDir();

module.exports = {
    uploadRoot,
    saveAvatarFromBase64,
    deleteAvatarFile
};
