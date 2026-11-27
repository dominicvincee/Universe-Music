# UNIVERSE MUSIC

A no-login music discovery prototype with a cosmic neon interface. The Android app is being migrated to YouTube Music search and audio stream extraction through InnerTubeX.

## Current implementation
- YouTube Music search through the native Android InnerTubeX bridge
- Native Android audio extraction/playback, with play/pause, seek, volume, queue, and next/previous controls
- Responsive cosmic-neon interface for Android phones
- Local favorites and collections stored on the device
- No app account or user login required

## Android build
The APK workflow is intentionally manual while the YouTube Music + InnerTubeX migration is being completed. Do not install an APK until the migration has been compiled and tested.

The Android project uses the InnerTubeX library and its stream extractor. YouTube can change its private interfaces at any time, so search and playback availability are not guaranteed. The current implementation is not yet runtime-verified.

## Web version
The static GitHub Pages UI does not have access to the Android native bridge. Search and playback through InnerTubeX are available only inside a compatible APK. Opening the page directly in a normal browser will not provide native InnerTubeX playback.

## Important limitations
- InnerTubeX is an unofficial client for YouTube's interfaces and may break when YouTube changes them.
- Some streams may not be available to the current playback client or Android MediaPlayer.
- Favorites, queue, and collections are stored locally and do not sync across devices.
- Music and artwork availability remain subject to YouTube's terms and applicable rights. No ad-free playback is promised.
- Do not embed private API credentials in client-side code or an APK.

See [LICENSE.md](LICENSE.md) and [ASSET_REGISTER.md](ASSET_REGISTER.md) before distributing or monetizing the project.

Android build validation note: the app compile SDK is aligned with the workflow's installed Android SDK platform (API 36). The native MediaPlayer data source call uses the Activity as Android Context. The native YouTube Music + InnerTubeX bridge still requires an Android build and device playback test before it can be considered verified.


Build validation: API 37 compile SDK configuration.
