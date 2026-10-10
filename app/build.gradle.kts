plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "pl.dorysuj.gra"
    compileSdk = 35
    defaultConfig {
        applicationId = "pl.dorysuj.gra"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }
}
