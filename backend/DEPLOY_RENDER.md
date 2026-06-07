# Deploy Backend To Render With Railway MySQL

This branch is prepared for deploying the backend service to Render while using a Railway-hosted MySQL database.

## Render Service

Create a Render Web Service from this repository:

```text
Repository: PhNguyen-05/DO_AN_MinLish_App
Branch: deploy
Root Directory: backend
Runtime: Docker
Dockerfile Path: ./backend/Dockerfile
Docker Build Context: ./backend
```

You can also use the root-level `render.yaml` blueprint. It defines the same Docker service and environment variable names.

## Railway MySQL Environment Variables

Set these variables in the Render dashboard:

```env
DB_HOST=acela.proxy.rlwy.net
DB_PORT=58234
DB_USER=root
DB_PASSWORD=<Railway database password>
DB_NAME=minlish_db
```

Do not commit the real `DB_PASSWORD` to GitHub. Add it only in Render Environment Variables.

If your Railway database name is different from `minlish_db`, update `DB_NAME` in Render.

## Other Render Environment Variables

```env
NODE_ENV=production
GOOGLE_CLIENT_ID=1018329968245-ak7cqp2ire3arj9ef7o4f85jqia0ci31.apps.googleusercontent.com
JWT_SECRET=<generate a long random string>
ENABLE_EMAIL_REMINDERS=true
REMINDER_TIME_ZONE=Asia/Bangkok
ENABLE_KEEP_ALIVE=true
KEEP_ALIVE_URL=https://do-an-minlish-app.onrender.com/health/db
KEEP_ALIVE_INTERVAL_MINUTES=10
```

Render Free blocks outbound SMTP ports, so production email should use the Google Apps Script mail relay:

```env
MAIL_PROVIDER=google_script
GOOGLE_SCRIPT_MAIL_URL=https://script.google.com/macros/s/AKfycbx7LgR2YaragbesI-BTZ8RMCqTdvN1Xah0aii1C6gMN5qKEuP2eTGIKzOPSvPiwy4Bm/exec
GOOGLE_SCRIPT_MAIL_SECRET=minlish_mail_secret_2026
```

The `deploy` branch already has these Google Apps Script values as production defaults. If Render has an old `MAIL_PROVIDER=brevo` variable, replace it with `MAIL_PROVIDER=google_script` or delete it.

For local development or non-Render hosts that allow SMTP, Gmail SMTP can still be used:

```env
MAIL_PROVIDER=smtp
SMTP_HOST=smtp.gmail.com
SMTP_PORT=465
SMTP_SECURE=true
SMTP_FAMILY=4
SMTP_USER=<your email>
SMTP_PASS=<your app password>
SMTP_FROM=<your email>
```

## Health Check

After deployment, open:

```text
https://<your-render-service>.onrender.com/health
```

Expected response:

```json
{"status":"ok"}
```

To verify database connectivity, open:

```text
https://<your-render-service>.onrender.com/health/db
```

Expected response:

```json
{"status":"ok","database":"ok"}
```

To verify the configured email provider, open:

```text
https://<your-render-service>.onrender.com/health/email
```

For Google Apps Script, the expected provider is:

```json
{"provider":"google_script","status":"configured"}
```

## Android API URL

After Render gives you a public URL, update the Android app's `BASE_URL` if you want the app to call the deployed backend:

```text
https://<your-render-service>.onrender.com/
```
