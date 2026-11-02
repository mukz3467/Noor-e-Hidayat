# Noor-e-Hidayat

Noor-e-Hidayat is an Android Islamic companion app built with Kotlin, Jetpack Compose and Material 3.

## Implemented features

- Native home dashboard with an emerald, gold and cream visual theme.
- Quran catalogue with search across surah names, translations and numbers.
- Surah reader with Uthmani Arabic text, English Sahih International translation and Urdu Jalandhry translation.
- Surah audio playback through AndroidX Media3 / ExoPlayer using AlQuran Cloud audio.
- Recitation catalogue.
- Beginner Quran-learning guidance covering Arabic letters, harakat, makharij, introductory Tajweed topics and revision.
- Basic Islamic knowledge quiz with scoring and replay.
- Makkah and Madinah broadcaster pages displayed in the in-app WebView.
- Favourites, lightweight recently played-title history and saved language preference.
- Adaptive anchored AdMob banner on standard screens; banner is hidden on Quran Reader and Media Player screens.
- In-app WebView for provider-controlled web/video content.

## Data and service dependencies

- Quran text and translations: AlQuran Cloud API (https://alquran.cloud/api).
- Quran audio: AlQuran Cloud audio CDN (https://cdn.islamic.network/quran/audio-surah/).
- Live broadcasts and YouTube playback depend on the relevant provider, internet connection, region, content settings and embedding permissions.

## Important limitations before public release

- Internet is required for Quran catalogue/reader data, streaming audio, live pages and embedded video.
- YouTube playback must use the official embedded player. YouTube controls, branding, provider ads, sign-in prompts and regional/age/content restrictions remain under YouTube's control.
- The in-app video category framework is present, but curated playlist IDs are not yet populated. Do not treat the empty categories as a verified video catalogue.
- Live broadcaster pages are external services and must be tested on real devices and networks.
- The language preference is saved, but full interface translations are not implemented yet.
- Watch history stores titles only; it is not a full playback-position history.
- The learning section is introductory guidance and does not replace a qualified Quran/Tajweed teacher.
- The repository workflow verifies compilation and APK packaging only. It does not perform emulator, device, playback, accessibility, privacy or policy QA.
- The manifest currently contains Google's official AdMob test App ID and the app uses Google's official test banner unit ID. Replace these with the app owner's production IDs only after the AdMob account/app setup and policy review are complete. Never ship test IDs as a revenue configuration.
- Before public distribution, prepare a compliant privacy policy, store listing, content/source review, production AdMob configuration and device QA.

## Android build

- Application ID: `com.noorehidayat.app`
- Minimum Android SDK: 24
- Compile / target SDK: 35
- Java / Kotlin JVM target: 17
- Version: 1.0.0 (versionCode 6)

GitHub Actions builds a debug APK on pushes to `main`. Download the artifact from the latest successful **Android build** workflow run. Debug APKs are for testing, not a signed Play Store release.

## Source links

- AlQuran Cloud API: https://alquran.cloud/api
- AndroidX Media3: https://developer.android.com/media/media3
- Google Mobile Ads SDK: https://developers.google.com/admob/android/quick-start
- YouTube embedded player requirements: https://developers.google.com/youtube/terms/required-minimum-functionality
