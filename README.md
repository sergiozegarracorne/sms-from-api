# Sms From API

Android app that polls an API endpoint for JSON payloads containing phone numbers and messages, stores them in a local SQLite database, and sends pending messages via SMS every 10 seconds. The API URL and service control are configurable from the UI.

## Building

This project uses Gradle and the Android Gradle plugin. Run `gradle assembleDebug` to build.
