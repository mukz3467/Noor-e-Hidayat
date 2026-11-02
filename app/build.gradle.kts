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
        versionCode = 5
        versionName = "0.8.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
