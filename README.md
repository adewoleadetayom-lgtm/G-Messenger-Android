# G Messenger v25 — combined build

G Messenger v25 combines the v21/v22 account-isolation and UI/function fixes with the three major production-direction upgrades requested for v25:

1. **Real cross-device messaging backend:** optional Firebase Realtime Database integration for registered-user discovery and text-message synchronization.
2. **Real internet video calling:** WebRTC peer-to-peer media with Firebase Realtime Database signaling and a public STUN server. A TURN service is recommended for difficult networks.
3. **True on-device Gemma path:** Android now includes MediaPipe LLM Inference support and a model-import flow for a compatible Gemma `.task` bundle. The model itself is intentionally not bundled in this ZIP because model files are large and licensing/model distribution must be handled separately.

## v25 General G Messenger group

- Every verified G Messenger account automatically gets a **G Messenger** official group at the top of Chats.
- All members can read the official messages.
- Only the configured G Messenger admin ID `gm_goodluck` can post to the official group.
- With Firebase configured, official messages are synchronized across devices through `generalMessages`.
- The group remains visible in local/offline mode, but cross-device messages require Firebase configuration.

## v25 Gemma response fix

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

The v25 sample uses anonymous Firebase Authentication plus the app's existing local verification flow. Before a public launch, replace the prototype verification with a real authenticated registration/OTP flow and review the database rules with a security professional.

## Gemma setup

Open **Gemma AI** and choose **Import Gemma .task model**. The imported compatible MediaPipe Task Bundle is copied into the app's private model directory and used by the Android LLM inference engine. If no model is installed, Gemma AI remains available as a setup screen rather than pretending that a neural model is running.

Google's current mobile documentation describes running Gemma on Android with the MediaPipe LLM Inference API, and its conversion guide describes `.task` bundles for on-device Android inference. See the official Google AI Edge documentation before choosing and distributing a model.

## Build

The project is version **24.0.0** / versionCode **24**. GitHub Actions builds the debug APK.

The v25 implementation is intentionally honest about external configuration: Firebase credentials and a Gemma model bundle are required for those production-direction features to operate across devices.
