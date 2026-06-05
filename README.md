# MinLish App

MinLish is a vocabulary learning system built with an Android application and a Node.js REST API. It supports email authentication, Google Sign-In, flashcard learning, progress tracking, study reminders, and vocabulary import/export.

## Project Components

- `MinLishApp/`: Android application built with Kotlin, Jetpack Compose, MVVM, Retrofit, Room, and Credential Manager.
- `backend/`: REST API built with Node.js, Express, MySQL, JWT, bcrypt, Nodemailer, and Google Auth Library.
- `scripts/`: utility scripts for file encoding tasks.
- `backend/public/`: static sample images and audio files for learning content.
- `backend/uploads/`: runtime upload directory, such as user avatars.

## Main Features

- Register and log in with email/password.
- Log in or register with Google Sign-In.
- Manage user profile, learning goals, and settings.
- View dashboard data such as learning progress, streak, learned words, and accuracy.
- Learn vocabulary with daily plans, flashcards, and review sessions.
- Receive in-app study reminders and optional email reminders.
- Import and export vocabulary sets with CSV, Excel, and PDF-related workflows.
- Load pronunciation audio, images, and user avatars from the backend.

## Tech Stack

Android:

- Kotlin
- Jetpack Compose
- Retrofit + Gson
- Room
- Coil
- AndroidX Credentials + Google ID

Backend:

- Node.js
- Express
- MySQL2
- JWT
- bcryptjs
- google-auth-library
- Nodemailer
- dotenv

## Folder Structure

```text
DO_AN_MinLish_App/
|-- MinLishApp/
|   |-- app/
|   |   `-- src/main/java/com/minlish/app/
|   |       |-- data/        # models, API service, repositories, local database
|   |       |-- feature/     # screens and ViewModels grouped by feature
|   |       |-- ui/          # Compose theme
|   |       `-- utils/       # pronunciation and shared helpers
|   |-- debug/               # shared debug keystore for Google Sign-In
|   `-- gradle.properties    # shared Android configuration
|-- backend/
|   |-- src/
|   |   |-- config/          # environment, database, and mail config
|   |   |-- controllers/     # REST API controllers
|   |   |-- jobs/            # scheduled jobs
|   |   |-- middlewares/     # auth and error handling
|   |   |-- routes/          # route definitions
|   |   |-- services/        # business logic
|   |   `-- utils/           # shared helpers
|   |-- public/              # static images/audio
|   |-- uploads/             # runtime uploads
|   `-- server.js            # backend entrypoint
|-- scripts/
|-- ProjectDescription.md
`-- README.md
```

## Environment Requirements

- Android Studio with Android SDK.
- An Android emulator or real Android device.
- JDK compatible with the Android Gradle wrapper.
- Node.js 18 or newer.
- MySQL Server.
- Google Play Services on the Android device/emulator for Google Sign-In.

## Backend Setup

Install backend dependencies:

```powershell
cd backend
npm install
```

Create a local `.env` file if it does not exist:

```powershell
Copy-Item .env.example .env
```

Important environment variables:

```env
PORT=3000
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=123456
DB_NAME=minlish_db
GOOGLE_CLIENT_ID=1018329968245-ak7cqp2ire3arj9ef7o4f85jqia0ci31.apps.googleusercontent.com
SMTP_HOST=smtp.gmail.com
SMTP_PORT=465
SMTP_SECURE=true
SMTP_USER=your-email@gmail.com
SMTP_PASS=your-app-password
SMTP_FROM=your-email@gmail.com
ENABLE_EMAIL_REMINDERS=true
REMINDER_TIME_ZONE=Asia/Bangkok
```

`GOOGLE_CLIENT_ID` also has a default fallback in `backend/src/config/env.js`, so team members do not need to configure a new Google OAuth client just to run Google login. SMTP only needs real credentials if the team wants email OTP or email reminders to work.

Create the MySQL database:

```sql
CREATE DATABASE minlish_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

If the team has a database dump, import it into `minlish_db` before starting the backend.

Start the backend:

```powershell
npm start
```

By default, the backend runs at:

```text
http://localhost:3000
```

## Android Setup

Open the `MinLishApp/` folder in Android Studio, sync Gradle, then run the app.

The debug API URL is configured in `MinLishApp/app/build.gradle.kts`:

```text
http://192.168.0.105:3000/
```

This is the LAN IP of the machine running the backend. If the backend runs on the same host machine as an Android emulator, you can change it to `http://10.0.2.2:3000/`. If you use a real phone, the phone and backend machine must be on the same Wi-Fi network.

`MinLishApp/local.properties` must not be committed because it contains a machine-specific Android SDK path. Android Studio creates it automatically when opening the project.

## Google Sign-In

The project uses a shared debug keystore:

```text
MinLishApp/debug/minlish-debug.keystore
```

Because this keystore is committed to the repository, all team members use the same debug certificate. They do not need to register their own machine-specific SHA-1 fingerprint.

Current Google Cloud OAuth configuration:

```text
Web Client ID:
1018329968245-ak7cqp2ire3arj9ef7o4f85jqia0ci31.apps.googleusercontent.com

Android package:
com.minlish.app

Android SHA-1:
ED:78:27:46:42:B6:81:42:A4:93:06:AF:90:63:D2:A1:BF:9F:6B:1A
```

The Android app and backend both use the Web Client ID. The Android Client ID is not copied into the code; it only needs to exist in the same Google Cloud project so Google can verify the Android package name and SHA-1 certificate.

If a device already has an older build installed before the shared keystore change, uninstall the old app once:

```powershell
adb uninstall com.minlish.app
```

## Main API Endpoints

Base URL:

```text
http://<backend-host>:3000/
```

Authentication:

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/google
POST /api/auth/forgot-password
POST /api/auth/reset-password
```

Dashboard:

```text
GET /api/dashboard
GET /api/dashboard/progress
```

Learning:

```text
GET  /api/learning/plan
GET  /api/learning/decks
GET  /api/learning/session
POST /api/learning/review
```

User:

```text
GET /api/user/profile
PUT /api/user/profile
GET /api/user/settings
PUT /api/user/settings
```

Notifications:

```text
GET  /api/notifications/summary
POST /api/notifications/email/reminder
```

Protected endpoints require this header:

```text
Authorization: Bearer <jwt-token>
```

## Quick Start For Team Members

1. Clone the repository.
2. Import the MySQL database dump if the team provides one.
3. Start the backend:

```powershell
cd backend
npm install
npm start
```

4. Open `MinLishApp/` in Android Studio.
5. Sync Gradle and run the Android app.
6. If the device already has an older build installed, uninstall `com.minlish.app` once and run again.

## Common Issues

`Google Cloud chua dang ky SHA debug cua app.`

- Check that the Android OAuth Client in Google Cloud uses package `com.minlish.app`.
- Check that the registered SHA-1 matches the value in this README.
- Check that the app was reinstalled after switching to the shared debug keystore.
- Check that the Web Client ID in `gradle.properties` and the backend belongs to the same Google Cloud project.

`INSTALL_FAILED_UPDATE_INCOMPATIBLE`

- The old installed app was signed by a different debug key. Uninstall it once:

```powershell
adb uninstall com.minlish.app
```

`Cannot connect to server`

- Check that the backend is running.
- Check `BASE_URL` in `MinLishApp/app/build.gradle.kts`.
- For a real phone, make sure the phone and backend machine are on the same network.
- For an emulator with backend on the host machine, use `http://10.0.2.2:3000/`.

`Backend chua cau hinh GOOGLE_CLIENT_ID`

- Check `.env` or the fallback value in `backend/src/config/env.js`.
- Restart the backend after changing `.env`.

## Git Notes

Do not commit runtime or machine-local files:

```text
backend/.env
backend/node_modules/
MinLishApp/local.properties
MinLishApp/.gradle/
MinLishApp/app/build/
```

Commit shared configuration files:

```text
MinLishApp/gradle.properties
MinLishApp/debug/minlish-debug.keystore
backend/.env.example
```
