# Bruce Care – Android app

A real Android app (built with Capacitor) with reminders that fire even when the app is closed,
and optional sync between phones.

## Get the APK (once, ~10 min)
1. Create a free account at github.com and make a **new repository** (private is fine).
2. Upload everything in this folder to it (drag the files in on the web page:
   Add file → Upload files). Include the hidden `.github` folder – if the web uploader skips it,
   use GitHub Desktop or `git push` instead.
3. Open the repo → **Actions** tab → "Build Android APK" → **Run workflow**. Wait ~5 min for the green tick.
4. On your phone, open the repo → **Releases** → *Bruce Care (latest)* → download `bruce-care.apk`.
5. Open the downloaded file. Android will ask to allow installs from your browser – allow it, then Install.
6. First launch: tap **Allow** for notifications.

To update the app later, change files in the repo; the build re-runs and replaces the "latest" release.
Install the new APK over the old one – your data is kept.

## Sync between phones (optional)
Follow `SYNC.md`, then on each phone open the app → bottom → "Sync across devices".

## Notes
- Reminders have **Done** and **Snooze 30 min** buttons right in the notification.
- If reminders arrive late, set the app's battery usage to "Unrestricted" (Settings → Apps → Bruce Care → Battery).
- This is a debug-signed build for personal use (not for the Play Store).
