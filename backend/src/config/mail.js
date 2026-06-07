const nodemailer = require('nodemailer');
const dns = require('dns');
const env = require('./env');

let transporter = null;

if (typeof dns.setDefaultResultOrder === 'function') {
    dns.setDefaultResultOrder('ipv4first');
}

if (env.smtp.user && env.smtp.pass) {
    transporter = nodemailer.createTransport({
        host: env.smtp.host,
        port: env.smtp.port,
        secure: env.smtp.secure,
        family: env.smtp.family,
        connectionTimeout: 10000,
        greetingTimeout: 10000,
        socketTimeout: 20000,
        auth: {
            user: env.smtp.user,
            pass: env.smtp.pass
        }
    });
} else if (env.mailProvider === 'smtp') {
    console.warn('SMTP credentials not provided. Forgot-password emails will fail until SMTP_USER and SMTP_PASS are set.');
}

module.exports = transporter;
