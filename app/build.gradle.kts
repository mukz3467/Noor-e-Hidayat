plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.noorehidayat.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.noorehidayat.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 4
        versionName = "0.7.0"
    }
}
