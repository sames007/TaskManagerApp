# Task Management System

A JavaFX desktop application for organizing personal, school, work, and family tasks. The app supports task creation, editing, deletion, completion tracking, CSV import/export, reminders, a calendar view, light/dark themes, profile pictures, and an optional AI assistant.

## Features

- Create, edit, complete, and delete tasks.
- Track due dates, due times, categories, priorities, status, and optional reminders.
- View scheduled tasks in a calendar and upcoming-task panel.
- Import and export tasks as CSV files.
- Receive local notifications for incomplete tasks due within 24 hours.
- Switch between light and dark themes.
- Use the app without a database through offline local JSON storage.
- Use Gemini-powered AI assistance when an API key is configured.

## Offline Mode

The app does not require the database to run. If database credentials are missing or the database is unreachable, login and sign-up continue in offline mode and tasks are stored locally at:

`%USERPROFILE%\.taskmanagerapp\tasks.json`

Offline task data is separated by the email or username used at login.

## Requirements

- Java JDK 23 or newer
- Maven, or the included Maven wrapper
- JavaFX dependencies provided through Maven

## Configuration

Live credentials should never be committed to source control. Configuration can be supplied through environment variables, Java system properties, or an ignored `config.local.properties` file in the project root.

Optional database settings:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `DB_SSL_MODE` (defaults to `REQUIRED`)

Optional AI assistant setting:

- `GEMINI_API_KEY`
- `GOOGLE_API_KEY`
- `API_KEY`

## Run

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-24"
.\mvnw.cmd javafx:run
```

## Test And Build

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-24"
.\mvnw.cmd test
.\mvnw.cmd package
```

The packaged jar is created under `target/`.

## Security Notes

- API keys and database credentials are loaded from external configuration.
- Passwords are hashed with PBKDF2 before database storage.
- Older plaintext database passwords can still be verified and are upgraded after successful login.
- Profile-picture and import-file handling validates file type and size.
- Generated build output is ignored and should not be committed.

## Contributors

- Yohangel Adames: Project Manager and Tech Support
- Antonio Villani: Scribe and Software Developer
- Philippe Jean: Architect
- Saim Sameer: Quality Assurance
- Bennett Thomas: Quality Assurance and Tech Support
- Zabdial Nunez: Software Developer
