# G Messenger v27 — combined build

G Messenger v27 combines the v21/v22 account-isolation and UI/function fixes with the three major production-direction upgrades requested for v27:

1. **Real cross-device messaging backend:** optional Firebase Realtime Database integration for registered-user discovery and text-message synchronization.
2. **Real internet video calling:** WebRTC peer-to-peer media with Firebase Realtime Database signaling and a public STUN server. A TURN service is recommended for difficult networks.
3. **True on-device Gemma path:** Android now includes MediaPipe LLM Inference support and a model-import flow for a compatible Gemma `.task` bundle. The model itself is intentionally not bundled in this ZIP because model files are large and licensing/model distribution must be handled separately.

## v27 General G Messenger group

- Every verified G Messenger account automatically gets a **G Messenger** official group at the top of Chats.
- All members can read the official messages.
- Only the configured G Messenger admin ID `gm_goodluck` can post to the official group.
- With Firebase configured, official messages are synchronized across devices through `generalMessages`.
- The group remains visible in local/offline mode, but cross-device messages require Firebase configuration.

## v27 Gemma response fix

- Uses Gemma 3 instruction-turn formatting (`<start_of_turn>user` → `<end_of_turn>` → `<start_of_turn>model`) for the imported Gemma 3 270M IT model. Google documents this formatter for Gemma instruction-tuned models.
- Keeps inference calls serialized on the existing single-thread executor so a second request cannot overlap an active response generation.
- Normalizes escaped/newline output and replaces empty responses with a visible retry message instead of a blank bubble.
- Keeps one global JavaScript result handler so multiple queued requests do not overwrite each other's callbacks.
- Quick actions now insert a real task prompt (Summarize / Explain / Rewrite / Translate) and leave the cursor ready for the text to process.
- The existing `gm23_` local-storage namespace is intentionally preserved so upgrading from v23 does not discard the user's local account data.

## What remains from the previous build

- Blue/white G Messenger branding and supplied logo.
- Per-account contacts/chats and isolated local data.
- Registered-user-only chat creation; unregistered phone contacts can be invited.
- Long-message wrapping and inline image/file previews.
- Self chat shown as **You / You yourself**.
- Updates/status, communities, channels, calls, settings and plans.
- Local fallback mode when Firebase is not configured.
- GitHub Actions APK workflow.

## Firebase setup

1. Create a Firebase project and enable **Anonymous Authentication** and **Realtime Database**.
2. Copy the project's web configuration into `app/src/main/assets/firebase-config.js`.
3. Deploy `firebase.rules.json` as the Realtime Database rules.
4. Build and install the app on two test devices.
5. Register/verify both users, add the other registered user as a contact, then send a text message or start a video call.

The app falls back to local-only behavior when the Firebase configuration is empty.

### Security note

v27 now uses real Firebase Phone Authentication for registration. The Android app requests an SMS OTP through Firebase, verifies the code with Firebase Authentication, and exchanges the resulting ID token for a Firebase custom token before the web app accesses the database. Anonymous Firebase sign-in is not used. The only administrator phone numbers are 09033229734 and 07067315898 (stored in normalized E.164 form on the server). Admin authorization is enforced with Firebase custom claims and Realtime Database Security Rules; the phone number alone is not trusted by the client UI.

## Gemma setup

Open **Gemma AI** and choose **Import Gemma .task model**. The imported compatible MediaPipe Task Bundle is copied into the app's private model directory and used by the Android LLM inference engine. If no model is installed, Gemma AI remains available as a setup screen rather than pretending that a neural model is running.

Google's current mobile documentation describes running Gemma on Android with the MediaPipe LLM Inference API, and its conversion guide describes `.task` bundles for on-device Android inference. See the official Google AI Edge documentation before choosing and distributing a model.

## Build

The project is version **27.0.0** / versionCode **27**. GitHub Actions builds the debug APK.

The v27 implementation is intentionally honest about external configuration: Firebase credentials and a Gemma model bundle are required for those production-direction features to operate across devices.


## v27 feature upgrade

Expanded chat, groups, communities, status media, wallpaper, admin panel, account-isolated local storage, and Firebase-backed group/message hooks. The official G Messenger logo is used for the official group avatar. Cross-device delivery requires a configured Firebase project and deployed security rules.

## Google authentication in v27

- Registration uses Google Sign-In through Android Credential Manager.
- Firebase Authentication receives the Google ID token.
- The app exchanges the verified Firebase ID token with the Cloud Functions backend.
- The backend marks the account with `google_verified=true` and grants `admin=true` only to the configured administrator Google emails.
- SMS phone verification is not used by v27.
- Phone numbers remain optional contact information rather than an authentication requirement.

## GitHub Actions

The v27 Android workflow builds the APK on GitHub Actions. It restores `google-services.json` from the `GOOGLE_SERVICES_JSON_B64` GitHub secret and uploads the debug APK as an artifact.

The Firebase deployment workflow deploys Realtime Database rules and Cloud Functions using the `FIREBASE_SERVICE_ACCOUNT_JSON` GitHub secret.


## v27 Firebase + GitHub build

This repository is configured for Google-only authentication, Firebase Realtime Database, Cloud Functions, FCM, and GitHub Actions APK builds. Phone/SMS authentication is not required by the v27 app.

### Required GitHub Actions secrets

- `GOOGLE_SERVICES_JSON_B64`: base64 contents of the Android `google-services.json` for package `com.gmessenger.app`.
- `FIREBASE_SERVICE_ACCOUNT_JSON`: Firebase/GCP service-account JSON with permission to deploy Realtime Database rules and Cloud Functions.

The Android workflow restores `google-services.json` only during CI and builds `app-debug.apk`. The Firebase workflow deploys `firebase.rules.json` and `functions/`.

### Build

Push to `v27-final` or run the **Build G Messenger v27 APK** workflow manually. The resulting APK is uploaded as a GitHub Actions artifact.

### Firebase deploy

Push Firebase files to `v27-final` or manually run **Deploy G Messenger Firebase**. Cloud Functions may require the Firebase project to be on a supported billing plan.
