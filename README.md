# Noor-e-Hidayat

Premium Islamic Android application built with Kotlin, Jetpack Compose and Material 3.

## Current implementation

- Native Compose home screen with emerald, gold and cream visual system.
- Quran catalogue loaded from AlQuran Cloud (114 surahs).
- Surah reader loads Uthmani Arabic text, English Sahih International translation and Urdu Jalandhry translation.
- Surah audio playback using AndroidX Media3 / ExoPlayer and the AlQuran Cloud audio CDN.
- Recitation catalogue, Quran learning guidance, basic Islamic quiz.
- Makkah and Madinah official broadcaster pages and YouTube content open in an in-app WebView.
- Favourites and language preference stored locally; watch history is currently a lightweight title history.
- Android minimum SDK 23; Java 17; Compose compiler plugin aligned to Kotlin 2.0.21.

## Important limitations

- Internet connection is required for Quran data, translations and streaming audio.
- YouTube embeds/search and broadcaster streams depend on provider permissions, region, network, ads and availability. The app does not bypass restrictions or remove branding.
- The language selector stores the preference but full translations of every UI screen are not implemented yet.
- Lecture catalogue is topic-based YouTube discovery, not a vetted scholarly catalogue. Verify religious rulings with qualified scholars.
- Live broadcast links must be tested on the target device/network.
- No user/device installation test has been completed by this repository workflow. A successful CI build is not the same as device QA.

## Build

GitHub Actions builds a debug APK on pushes to the main branch. Open the Actions tab and download the artifact from the latest successful run.

## Quran data sources

- AlQuran Cloud API: https://alquran.cloud/api
- AlQuran Cloud audio CDN: https://cdn.islamic.network/quran/audio-surah/
