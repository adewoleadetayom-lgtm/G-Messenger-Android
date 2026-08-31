# G Messenger — GitHub APK Build

This repository is prepared for GitHub Actions.

## Build the APK on GitHub

1. Create a GitHub repository named `G-Messenger-Android`.
2. Upload the **contents of this folder** to the repository (not this ZIP as a single file).
3. Commit the files to `main` (or `master`).
4. Open the repository's **Actions** tab.
5. Select **Build G Messenger APK**.
6. Click **Run workflow**.
7. Wait for the workflow to finish successfully.
8. Open the completed workflow run.
9. Under **Artifacts**, download `G-Messenger-debug`.
10. Extract the downloaded artifact to obtain `app-debug.apk`.

## Important

This is a native Android starter project for G Messenger. The GitHub workflow builds a debug APK.

The project includes the G Messenger foundation: registration/session persistence, Chats, Updates, Communities, Calls, contact permission, attachment picker, camera/microphone permissions, profile/session UI, and a Gemma AI interface.

Real production messaging between devices, SMS verification, push notifications, secure media storage, production WebRTC voice/video calls, and a real AI model/API require backend/service integration and appropriate credentials. The project does not contain secret API keys.

## Local/Termux build

If Gradle and an Android SDK are installed locally:

    gradle assembleDebug

The APK will be:

    app/build/outputs/apk/debug/app-debug.apk
