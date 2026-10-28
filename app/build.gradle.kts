plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "home.garden.presence"
    compileSdk = 36
    defaultConfig {
        applicationId = "home.garden.presence.preview"
        minSdk = 29
        targetSdk = 36
        versionCode = 11
        versionName = "0.7.0-garden-editorial"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.core:core:1.10.1")
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.webkit:webkit:1.8.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
