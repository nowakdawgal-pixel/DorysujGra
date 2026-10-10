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
        versionCode = 2
        versionName = "2.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
