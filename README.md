# Reddit Image Browser 0.1.2 — Android Studio source

Open this folder in Android Studio and let Gradle sync. Run on an emulator or phone. Alternatively run `build.bat` on Windows.

## What changed
- Full-screen viewer: ordinary single-finger drags are handled by `HorizontalPager` instead of being intercepted by the zoom detector.
- On-screen ◀ ▶ buttons for easy emulator/mouse testing.
- Account dialog with Reddit OAuth 2 authorization-code + PKCE scaffolding and callback handling.
- Last selected subreddit persists across launches.

## Reddit login setup and limitations
- **The project builds without credentials**, but Sign in needs your own Reddit-approved installed-app OAuth client ID.
- In `app/build.gradle.kts`, replace the empty value in `buildConfigField("String", "REDDIT_CLIENT_ID", "\\\"\\\"")` with the escaped quoted client ID, for example `"\\\"YOUR_CLIENT_ID\\\""`.
- Register `ribrowser://oauth` as the exact redirect URI, if Reddit permits that URI for your app.
- Reddit's app registration, authorization policies, and PKCE support must be confirmed for your approved app; this flow is **not yet tested against Reddit**.
- The prototype uses a temporary OAuth grant and does **not** persist access tokens. A successful response marks a local `connected` flag only, not a reusable authenticated session. Login state is therefore **demonstration-only** and must not be interpreted as an active Reddit API session.
- Live Reddit feeds, account subscriptions, and real saved posts are not implemented. Images still come from `picsum.photos`.
- Sample photos require internet. Pinch zoom is experimental; normal swiping and arrow navigation should work.

## Build
Open the folder containing `settings.gradle.kts` in Android Studio, then Build > Build APK(s). Output: `app/build/outputs/apk/debug/app-debug.apk`. No compiled APK is included.


## Build 0.1.2 — emulator mouse drag fix

- Full-screen image viewer: hold the **left mouse button** over an image, drag at least 65 pixels horizontally, and release to move to the next or previous image.
- On a phone, swipe left/right normally. In the emulator, mouse input configured as simulated touch will also use the normal Android swipe gesture.
- The **◀ / ▶** buttons remain available for navigation.
- Click an image without dragging to show/hide the controls (mouse mode).
- Pinch zoom continues to use two-finger touch input.
- Reddit OAuth source and the last-viewed subreddit setting are unchanged. The grid still shows sample images until Reddit API access is approved.
