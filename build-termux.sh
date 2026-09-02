#!/data/data/com.termux/files/usr/bin/bash
set -e
echo "G Messenger v21 build"
if ! command -v gradle >/dev/null; then echo "Gradle is not installed. Install/configure Gradle + Android SDK first."; exit 1; fi
gradle --no-daemon clean :app:assembleDebug
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
