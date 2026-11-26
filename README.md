# UNIVERSE MUSIC

A no-login music discovery and streaming prototype with a cosmic neon interface.

## Current prototype
- Trending music discovery through the Audius API
- Search by track or artist
- In-page audio player with previous / play-pause / next controls
- Local favorites saved in the current browser/device
- Responsive layout for Android phones and desktop browsers
- No app account or user login

## Run locally
Open `index.html` in a browser or publish the static files using a static hosting service. Internet access is required for catalog search and streaming. Some browsers may restrict API/audio requests when opening a local file; if that happens, use a static web host.

## Important limitations
- The streaming catalog and playback availability depend on the third-party provider.
- Favorites are stored locally and do not sync across devices.
- API availability does not grant rights to all music or artwork. Review current provider terms and individual track permissions before commercial use.
- Do not embed private API credentials in client-side code or an APK.

See [LICENSE.md](LICENSE.md) and [ASSET_REGISTER.md](ASSET_REGISTER.md) before distributing or monetizing the project.
