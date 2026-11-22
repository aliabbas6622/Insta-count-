# ReelGauge

Battery-optimized Android Instagram Reel counter with a top-center floating progress pill, persistent daily usage notification, customizable daily limits, and 7-day streak insights.

## One-Click APK Download (GitHub)

1. Push this project to GitHub (`main` or `master` branch).
2. The **Build ReelGauge APK** GitHub Actions workflow runs automatically and publishes `ReelGauge.apk` to **Releases -> ReelGauge Latest APK**.
3. On your repository's GitHub page, open **Releases** (right sidebar) and click **`ReelGauge.apk`** for a one-click download—or share the direct link:
   `https://github.com/<your-username>/<your-repo>/releases/latest/download/ReelGauge.apk`

> You can also download the built APK directly from AI Studio's **Settings / Export APK** menu or from the local `app/build/outputs/apk/debug/app-debug.apk` output.

## Local Build

```bash
gradle :app:assembleDebug
```
