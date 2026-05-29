function notFoundHandler(req, res) {
    res.status(404).json({ message: 'Không tìm thấy endpoint.' });
}

function errorHandler(err, req, res, next) {
    const statusCode = err.statusCode || 500;
    const responseBody = err.statusCode
        ? { message: err.message }
        : { error: err.message };

    res.status(statusCode).json(responseBody);
}

module.exports = {
    notFoundHandler,
    errorHandler
};
