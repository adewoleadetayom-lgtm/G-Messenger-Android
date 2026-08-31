#!/data/data/com.termux/files/usr/bin/bash
set -e
echo "G Messenger Android build"
if command -v gradle >/dev/null 2>&1; then
  gradle assembleDebug
else
  echo "Gradle is not installed. Install it in your Android/Termux build environment first."
  exit 1
fi
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
