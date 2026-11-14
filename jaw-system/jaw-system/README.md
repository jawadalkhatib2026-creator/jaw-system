# Jaw System

An Apple TV-style home screen (launcher) for Android TV. English + Arabic (follows the TV language).

- Big clock and date, rounded app cards that grow when focused
- Sultan Settings > Add apps: pick which apps appear on the home screen. Hold OK on an app to remove it
- Settings tile: the TV's original settings
- TV channel tiles: add a channel (name, optional link, the app that opens it, optional logo picture)
- Wallpaper from your own picture (Sultan Settings > Wallpaper)
- Free up memory button (Sultan Settings) closes background apps
- Connection check, speed test, Wi-Fi settings and app-cache shortcuts (Sultan Settings)
- Liquid Glass look, wallpaper from phone via QR (also TV files and ready-made wallpapers), Jaw Share (phone to TV via QR), Jaw AppStore, Jaw Accounts + admin panel, account type (Jaw / Google / Both)
- Sultan Settings tile: theme, 12/24-hour clock, reset

## Build the APK
- Android Studio: open this folder, Build > Build APK(s)
- Or push this folder to GitHub: the "Build APK" action produces `sultan-system-apk` under Actions > Artifacts

## Install on the TV
Send the APK to the TV (USB stick, or `adb install app-release.apk`), open it once, then press Home and pick Jaw System as the home app.
