const app = require('./src/app');
const env = require('./src/config/env');
const { startDailyEmailReminderJob } = require('./src/jobs/notification.job');

app.listen(env.port, () => {
    console.log(`MinLish backend is running at http://localhost:${env.port}`);
    startDailyEmailReminderJob();
});
