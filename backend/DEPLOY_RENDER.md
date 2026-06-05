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
DB_NAME=railway
```

Do not commit the real `DB_PASSWORD` to GitHub. Add it only in Render Environment Variables.

If your Railway database name is different from `railway`, update `DB_NAME` in Render.

## Other Render Environment Variables

```env
NODE_ENV=production
GOOGLE_CLIENT_ID=1018329968245-ak7cqp2ire3arj9ef7o4f85jqia0ci31.apps.googleusercontent.com
JWT_SECRET=<generate a long random string>
ENABLE_EMAIL_REMINDERS=false
REMINDER_TIME_ZONE=Asia/Bangkok
```

SMTP is optional. Configure it only if forgot-password email or email reminders must work:

```env
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_SECURE=false
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

## Android API URL

After Render gives you a public URL, update the Android app's `BASE_URL` if you want the app to call the deployed backend:

```text
https://<your-render-service>.onrender.com/
```
