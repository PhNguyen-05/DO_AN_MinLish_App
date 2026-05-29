const jwt = require('jsonwebtoken');
const env = require('../config/env');

function authenticateToken(req, res, next) {
    const authHeader = req.headers.authorization;
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
        return res.status(401).json({ message: 'Quyền truy cập bị từ chối do thiếu Token xác thực!' });
    }

    jwt.verify(token, env.jwtSecret, (err, decodedUser) => {
        if (err) {
            return res.status(403).json({ message: 'Token của bạn đã hết hạn hoặc không hợp lệ!' });
        }

        req.user = decodedUser;
        next();
    });
}

module.exports = authenticateToken;
