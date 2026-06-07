const transporter = require('../config/mail');
const env = require('../config/env');
const { createHttpError } = require('../utils/httpError');

const SEND_TIMEOUT_MS = 25000;
const BREVO_TIMEOUT_MS = 25000;
const GOOGLE_SCRIPT_TIMEOUT_MS = 25000;

function sendWithTimeout(mailOptions) {
    return Promise.race([
        transporter.sendMail(mailOptions),
        new Promise((_, reject) => {
            setTimeout(() => reject(createHttpError(504, 'SMTP gui email qua thoi gian cho.')), SEND_TIMEOUT_MS);
        })
    ]);
}

async function sendMail(mailOptions) {
    if (env.mailProvider === 'google_script') {
        await sendGoogleScriptMail(mailOptions);
        return;
    }

    if (env.mailProvider === 'brevo') {
        await sendBrevoMail(mailOptions);
        return;
    }

    if (env.mailProvider !== 'smtp') {
        throw createHttpError(500, `MAIL_PROVIDER khong duoc ho tro: ${env.mailProvider}`);
    }

    if (!transporter) {
        throw createHttpError(500, 'SMTP chua duoc cau hinh tren server. Vui long thiet lap SMTP_USER va SMTP_PASS.');
    }

    try {
        await sendWithTimeout(mailOptions);
    } catch (error) {
        if (error.statusCode) {
            throw error;
        }
        throw createHttpError(502, `Khong gui duoc email qua SMTP: ${error.message}`);
    }
}

async function sendGoogleScriptMail(mailOptions) {
    if (!env.googleScriptMail.url || !env.googleScriptMail.secret) {
        throw createHttpError(500, 'Google Apps Script mail chua duoc cau hinh tren server.');
    }

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), GOOGLE_SCRIPT_TIMEOUT_MS);

    try {
        const response = await fetch(env.googleScriptMail.url, {
            method: 'POST',
            headers: {
                'content-type': 'application/json'
            },
            body: JSON.stringify({
                secret: env.googleScriptMail.secret,
                to: mailOptions.to,
                subject: mailOptions.subject,
                text: mailOptions.text || '',
                html: mailOptions.html || mailOptions.text || ''
            }),
            signal: controller.signal
        });

        const bodyText = await response.text();
        const body = bodyText ? parseJsonBody(bodyText) : {};
        if (!response.ok || body.ok === false) {
            const message = body.message || bodyText || response.statusText;
            throw createHttpError(502, `Google Apps Script mail failed (${response.status}): ${message}`);
        }
    } catch (error) {
        if (error.statusCode) {
            throw error;
        }
        const message = error.name === 'AbortError' ? 'Google Apps Script mail qua thoi gian cho.' : error.message;
        throw createHttpError(502, `Khong gui duoc email qua Google Apps Script: ${message}`);
    } finally {
        clearTimeout(timeout);
    }
}

async function sendBrevoMail(mailOptions) {
    if (!env.brevo.apiKey) {
        throw createHttpError(500, 'Brevo chua duoc cau hinh tren server. Vui long thiet lap BREVO_API_KEY.');
    }
    if (!env.smtp.from) {
        throw createHttpError(500, 'Sender email chua duoc cau hinh. Vui long thiet lap SMTP_FROM.');
    }

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), BREVO_TIMEOUT_MS);

    try {
        const response = await fetch(`${env.brevo.baseUrl}/smtp/email`, {
            method: 'POST',
            headers: {
                accept: 'application/json',
                'api-key': env.brevo.apiKey,
                'content-type': 'application/json'
            },
            body: JSON.stringify({
                sender: {
                    name: 'MinLish',
                    email: env.smtp.from
                },
                to: [{ email: mailOptions.to }],
                subject: mailOptions.subject,
                textContent: mailOptions.text
            }),
            signal: controller.signal
        });

        const bodyText = await response.text();
        const body = bodyText ? parseJsonBody(bodyText) : {};
        if (!response.ok) {
            const message = body.message || bodyText || response.statusText;
            throw createHttpError(502, `Brevo email API failed (${response.status}): ${message}`);
        }
    } catch (error) {
        if (error.statusCode) {
            throw error;
        }
        const message = error.name === 'AbortError' ? 'Brevo email API qua thoi gian cho.' : error.message;
        throw createHttpError(502, `Khong gui duoc email qua Brevo API: ${message}`);
    } finally {
        clearTimeout(timeout);
    }
}

function parseJsonBody(bodyText) {
    try {
        return JSON.parse(bodyText);
    } catch {
        return {};
    }
}

async function verifyEmailProvider() {
    if (env.mailProvider === 'google_script') {
        if (!env.googleScriptMail.url || !env.googleScriptMail.secret) {
            throw createHttpError(500, 'Google Apps Script mail chua duoc cau hinh tren server.');
        }

        return { provider: 'google_script', status: 'configured' };
    }

    if (env.mailProvider === 'brevo') {
        if (!env.brevo.apiKey) {
            throw createHttpError(500, 'Brevo chua duoc cau hinh tren server. Vui long thiet lap BREVO_API_KEY.');
        }

        const response = await fetch(`${env.brevo.baseUrl}/account`, {
            headers: {
                accept: 'application/json',
                'api-key': env.brevo.apiKey
            }
        });
        if (!response.ok) {
            const bodyText = await response.text();
            throw createHttpError(502, `Brevo API key khong hop le hoac khong truy cap duoc (${response.status}): ${bodyText || response.statusText}`);
        }

        return { provider: 'brevo', status: 'ok' };
    }

    if (env.mailProvider !== 'smtp') {
        throw createHttpError(500, `MAIL_PROVIDER khong duoc ho tro: ${env.mailProvider}`);
    }

    if (!transporter) {
        throw createHttpError(500, 'SMTP chua duoc cau hinh tren server. Vui long thiet lap SMTP_USER va SMTP_PASS.');
    }

    await transporter.verify();
    return { provider: 'smtp', status: 'ok' };
}

async function sendPasswordResetOtp(email, otp) {
    await sendMail({
        from: `"MinLish" <${env.smtp.from}>`,
        to: email,
        subject: 'MinLish - Ma OTP dat lai mat khau',
        text: `Ma OTP cua ban la ${otp}. Ma co hieu luc trong 10 phut.`
    });
}

async function sendStudyReminderEmail(email, payload) {
    const name = payload.full_name || 'ban';
    const newWordsLeft = Number(payload.new_words_available || 0);
    const dueReviewCount = Number(payload.due_review_count || 0);
    const learnedToday = Number(payload.words_learned_today || 0);
    const dailyGoal = Number(payload.daily_new_words_goal || 0);
    const reviewedToday = Number(payload.words_reviewed_today || 0);
    const reviewGoal = Number(payload.daily_review_goal || 0);

    await sendMail({
        from: `"MinLish" <${env.smtp.from}>`,
        to: email,
        subject: 'MinLish - Nhac hoc tu vung hom nay',
        text: [
            `Chao ${name},`,
            '',
            'Da den luc quay lai MinLish de giu nhip hoc hom nay.',
            `Tu moi hom nay: ${learnedToday}/${dailyGoal}.`,
            `The on tap hom nay: ${reviewedToday}/${reviewGoal}.`,
            `Hien con ${newWordsLeft} tu moi co the hoc va ${dueReviewCount} the den han on.`,
            '',
            'Mo MinLish de tiep tuc hoc nhe.'
        ].join('\n')
    });
}

module.exports = {
    sendPasswordResetOtp,
    sendStudyReminderEmail,
    verifyEmailProvider
};
