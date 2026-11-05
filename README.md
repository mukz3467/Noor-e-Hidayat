# Noor-e-Hidayat

Noor-e-Hidayat is an Android Islamic companion app built with Kotlin, Jetpack Compose and Material 3.

## Implemented features

- Native home dashboard with an emerald, gold and cream visual theme.
- Quran catalogue with search across surah names, translations and numbers.
- Surah reader with Uthmani Arabic text and dynamically discovered text translation editions across the languages published by AlQuran Cloud, including Urdu and English where available.
- Tafseer reader that discovers text Tafseer editions from AlQuran Cloud and loads the selected edition by Surah; Urdu editions are prioritised when available.
- Location-based prayer timetable using the AlAdhan service, with selectable calculation methods and Hanafi/Shafi'i Asr settings.
- Qibla compass using the device location and rotation-vector sensor.
- Surah audio playback through AndroidX Media3 / ExoPlayer using AlQuran Cloud audio, with buffering/error feedback, retry controls and a MediaSession foreground service for background playback and system media controls.
- Optional offline Surah audio downloads through Android DownloadManager, with download notifications, private app storage and an offline playback library.
- Optional prayer-time notifications using inexact Android alarms, a notification channel and daily timetable refresh from the saved location; no exact-alarm permission is required.
- Optional daily Islamic reminder notification at 9:00 AM local time, with user opt-in, Android 13+ notification permission handling and an inexact alarm. Reminder wording is general encouragement, not a quoted Quran verse or Hadith.
- Recitation catalogue.
- Structured beginner Quran-learning path with Arabic letters, harakat, makharij, Noon/Meem rules, Madd, Qalqalah, Ghunnah, Waqf and revision.
- Twenty-question Islamic knowledge quiz with scoring and replay.
- Makkah and Madinah broadcaster pages displayed in the in-app WebView.
- Favourites, lightweight recently played-title history and saved language preference.
- Adaptive anchored AdMob test banner on standard screens; banner is hidden on Quran Reader and Media Player screens.
- Google UMP consent refresh on app launch, consent-form handling, consent-gated ad requests and a privacy-options entry point when required.
- In-app WebView for provider-controlled web/video content.

## Data and service dependencies

- Quran text and translations: AlQuran Cloud API (https://alquran.cloud/api).
- Quran audio: AlQuran Cloud audio CDN (https://cdn.islamic.network/quran/audio-surah/).
- Prayer times: AlAdhan API (https://aladhan.com/prayer-times-api).
- Live broadcasts and YouTube playback depend on the relevant provider, internet connection, region, content settings and embedding permissions.

## Important limitations before public release

- Internet is required for first-time Quran catalogue/reader downloads, streamed audio, live pages, video search and Tafseer editions. The catalogue and eight most recently read Surah/translation pairs are cached for offline text reading.
- Offline audio files are downloaded only when requested by the user and stored in app-private external music storage. Files are removed if the app is uninstalled or its app data is cleared. MediaSession foreground playback supports continuing audio when the app is backgrounded.
- Video discovery opens YouTube-hosted search pages inside the app; the app does not scrape, download or mirror videos. YouTube controls, branding, provider ads, sign-in prompts and regional/age/content restrictions remain under YouTube's control.
- Video categories currently open provider-side YouTube search results; there is no curated or independently verified playlist catalogue in the app yet.
- Live broadcaster pages are external services and must be tested on real devices and networks.
- The app-language preference is saved, but full interface localization is not implemented yet. Quran translation choice is separate.
- Watch history stores titles only; it is not a full playback-position history.
- The learning section is introductory guidance and does not replace a qualified Quran/Tajweed teacher.
- Prayer times depend on location permission, a location fix and the external AlAdhan service. Calculation method and Asr school are configurable; local mosque timetables may use different settings.
- Prayer and daily reminder notifications are optional and require notification permission on Android 13+. Android may delay inexact alarms due to battery optimisation; they are not guaranteed to fire at an exact second.
- Tafseer editions and their availability are supplied by AlQuran Cloud; verify religious interpretation with qualified scholars.
- The repository workflow verifies compilation and runs UI smoke tests on an emulator. It does not replace physical-device, playback, accessibility, privacy or policy QA.
- The app integrates Google's UMP SDK, but the publisher must still configure applicable privacy messages in AdMob. The app uses Google's official AdMob test App ID and test banner unit ID. Replace these with the app owner's production IDs only after the AdMob account/app setup and policy review are complete. Never ship test IDs as a revenue configuration.
- Before public distribution, prepare a compliant privacy policy, store listing, content/source review, production AdMob configuration and device QA.

## Android build

- Application ID: `com.noorehidayat.app`
- Minimum Android SDK: 24
- Compile / target SDK: 35
- Java / Kotlin JVM target: 17
- Version: 1.0.3 (versionCode 9)

GitHub Actions builds a debug APK on pushes to `main`. Download the artifact from the latest successful **Android build** workflow run. Debug APKs are for testing, not a signed Play Store release.

## Source links

- AlQuran Cloud API: https://alquran.cloud/api
- AndroidX Media3: https://developer.android.com/media/media3
- Media3 background playback: https://developer.android.com/media/media3/session/background-playback
- Google Mobile Ads SDK: https://developers.google.com/admob/android/quick-start
- Google UMP consent SDK: https://developers.google.com/admob/android/privacy
- YouTube embedded player requirements: https://developers.google.com/youtube/terms/required-minimum-functionality
