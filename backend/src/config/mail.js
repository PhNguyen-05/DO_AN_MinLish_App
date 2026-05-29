const nodemailer = require('nodemailer');
const env = require('./env');

let transporter = null;

if (env.smtp.user && env.smtp.pass) {
    transporter = nodemailer.createTransport({
        host: env.smtp.host,
        port: env.smtp.port,
        secure: env.smtp.secure,
        auth: {
            user: env.smtp.user,
            pass: env.smtp.pass
        }
    });
} else {
    console.warn('SMTP credentials not provided. Forgot-password emails will fail until SMTP_USER and SMTP_PASS are set.');
}

module.exports = transporter;
