# G Messenger v16 — Final UI + functional foundation

This project is a clean Android/Jetpack Compose implementation of the approved G Messenger reference design. The visual structure is kept intentionally close to the supplied reference: blue G Messenger branding, search/AI bar, filter chips, chat list, four-item bottom navigation, chat detail, Updates, Communities, Calls, New Contact, New Group, Gemma AI, Add Status, Settings and Profile.

## Functional in this build
- Screen-to-screen navigation
- Search/filtering of chats
- Open individual chat rooms
- Send local messages and update chat previews
- Create contacts and groups locally
- Create status updates locally
- Create community entries locally
- Calls/video-call actions wired to UI feedback (device call/video integration can be connected later)
- Gemma AI interaction layer with offline responses for Summarize/Explain/Rewrite/Translate
- Light/dark mode toggle
- Settings/profile actions and copy feedback
- Firebase Realtime Database dependency is included as the backend hook; add `google-services.json` and Firebase initialization/rules when connecting a real project.

## Important
The supplied screenshot is a visual reference, not executable source code. No earlier v15 source archive was attached to this turn, so this v16 is a self-contained implementation rather than a patch against unseen files. It deliberately does not replace the approved visual direction.

## Build
Open the folder in current Android Studio and let Gradle sync. The project uses Kotlin 2.3.21, Compose BOM 2026.08.00, Navigation Compose 2.10.0, compileSdk 36 and targetSdk 36.
