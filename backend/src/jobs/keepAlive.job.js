const env = require('../config/env');

let keepAliveTimer = null;

async function pingKeepAliveUrl() {
    const response = await fetch(env.keepAlive.url);
    if (!response.ok) {
        throw new Error(`Keep-alive request failed with status ${response.status}`);
    }
}

function startKeepAliveJob() {
    if (!env.keepAlive.enabled) {
        return;
    }
    if (!env.keepAlive.url) {
        console.warn('ENABLE_KEEP_ALIVE is true but KEEP_ALIVE_URL is not set.');
        return;
    }
    if (keepAliveTimer) {
        return;
    }

    const intervalMs = Math.max(env.keepAlive.intervalMinutes, 1) * 60 * 1000;
    keepAliveTimer = setInterval(() => {
        pingKeepAliveUrl().catch((error) => {
            console.warn(`Keep-alive ping failed: ${error.message}`);
        });
    }, intervalMs);

    if (typeof keepAliveTimer.unref === 'function') {
        keepAliveTimer.unref();
    }
}

module.exports = {
    startKeepAliveJob
};
