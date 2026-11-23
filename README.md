# iLock Music (prototype)

Android 9+ Kotlin prototype, targeting Android 15. Opens a lockscreen-visible activity manually; it **does not** automatically replace the system lockscreen player or launch on every lock event.

## Build
1. Open folder in Android Studio with JDK 17 and Android SDK 35 installed.
2. Let Gradle sync (internet needed for Gradle plugins).
3. Build > Build APK(s), or run `gradle assembleDebug` with a compatible local Gradle 8.9 installation.
4. Install the debug APK. Open the app and enable notification access for iLock Music in Android settings.
5. Start playback in Yandex Music, then open iLock Music. You can lock the screen while this activity is visible; it requests to remain visible above the keyguard.

## Limitations
- Not a system lockscreen replacement; launch must be user-initiated. Android restricts background launches.
- Yandex Music must expose an active media session and artwork/metadata. Seeking depends on supported transport controls.
- This is untested source code, not a compiled APK. Device-specific HyperOS behavior requires testing.
- Notification listener access exposes notification information to the app. This prototype only uses it to authorize active media-session access; it does not transmit data.
