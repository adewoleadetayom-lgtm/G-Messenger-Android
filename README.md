# G Messenger v21

G Messenger v18 is a phone-first Android messaging UI inspired by contemporary messaging apps, using G Messenger branding.

## What's included
- Registration + local verification flow
- Persistent profile and local app storage
- Separate chat rooms per contact
- Contact creation and Android phone-contact reader
- Invite unregistered phone contacts via SMS composer
- Attachments/photo picker, profile photo and status photos
- Updates/Status with Text, Music, Layout, Voice and Camera/Gallery entry points
- Communities and channels
- Calls screen with keypad, call history and native phone dialing
- Voice/microphone and camera permission bridge
- Chat options/settings
- General settings with Free / Paid data-saver plans and manual payment instructions
- Gemma AI assistant UI with offline/on-device architecture and action shortcuts
- Light/dark theme
- GitHub Actions APK build

## Important
The UI and local workflows are functional. Real internet messaging between different people requires a backend/realtime service; real WhatsApp-style internet voice/video calls require a signaling and media service; real on-device Gemma generation requires the Gemma runtime and model weights to be bundled/downloaded separately.

The current verification screen uses `123456` as a local prototype code. Replace it with a real OTP provider before public launch.

Paid-plan screens are informational/manual: payment is directed to the number shown by the app, and the app does not verify payments automatically.

## GitHub
Push the project to the main branch. GitHub Actions builds the v21 debug APK.


## v18 Phone-Friendly Polish
The approved v18 UI is preserved. Responsive CSS, Android safe-area handling, small-screen breakpoints, touch-friendly controls, and WebView scaling settings were added for Android phones, including narrow 320–380dp screens.

## v18 phone-friendly additions
- Preserves the v17 phone-first layout and blue/white G Messenger branding.
- Adds message Edit, Forward and Delete actions.
- Adds chat Delete and Mute actions.
- Makes status cards open a status viewer when tapped.
- Adds Log out in Settings.
- Keeps responsive 320–380dp Android-phone behavior and safe-area spacing.
- Keeps native Android phone dialing and media/contact permission bridges.

## Verification note
The local OTP in this demo remains a prototype. Real SMS OTP requires a configured authentication provider and project credentials; this archive does not pretend that local code is real SMS verification.


## v19 contact rule
Only users registered on G Messenger can be added to chats. Phone contacts who are not registered show **Invite** only; they cannot be added. A registered contact must match a registered phone number or G Messenger ID. This change is isolated to contact/chat creation logic and does not alter existing chats, updates, communities, calls, profile, settings, or Gemma UI.


The current client-side registry records verified users on this device. For production cross-device registration lookup, connect the same rule to the server/Firebase user directory; never trust a phone-contact entry itself as proof of registration.


## G Messenger v20 — Blue & White Logo
- Uses the supplied G Messenger logo recolored blue and white.
- Existing v19 registered-contact rule is preserved: only registered G Messenger users can be added; unregistered contacts can only be invited.
- Existing chats, updates, communities, calls, settings, profile, and Gemma AI UI are preserved.
- Android launcher icon now uses the supplied blue-and-white logo.


## G Messenger v21 — account isolation, safe chat bubbles and inline media
- Each registered profile has its own contacts and chats stored under a profile-specific local key. A newly registered account starts with no contacts or chats.
- Existing v19/v20 contact/chat data is migrated only to the profile that was active during the upgrade, preserving that user's existing work without sharing it with newly registered profiles.
- Only registered G Messenger users can be added to chats. Unregistered phone contacts can only be invited.
- Long messages wrap inside the chat bubble and never overflow the chat screen.
- Image attachments render inline in the chat; PDFs and other files show an inline file preview/card.
- A dedicated self-chat is available as **You**, with **You / yourself** shown in the header.
- Existing updates, communities, channels, calls, settings, Gemma AI, blue/white branding, and other v20 UI are preserved.

### v21 limitation
The registration directory remains local in this demo. For two separate physical devices to discover one another as registered users, the same registration/contact rule must be connected to a shared backend such as Firebase with authenticated user records.
