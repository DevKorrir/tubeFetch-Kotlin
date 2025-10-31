import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "dev.korryr.tubefetch"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.korryr.tubefetch"
        minSdk = 24
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig fields
        buildConfigField("String", "YOUTUBE_API_KEY", "\"${properties["YOUTUBE_API_KEY"]}\"")
        buildConfigField("String", "YOUTUBE_BASE_URL", "\"${properties["YOUTUBE_BASE_URL"]}\"")

    }

    // Load keys.properties
    val keysProperties = file("../keys.properties")
    if (keysProperties.exists()) {
        val keys = Properties()
        keys.load(keysProperties.inputStream())
        keys.forEach { key, value ->
            project.extra[key.toString()] = value
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //extend icons
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.57.2")
    ksp("com.google.dagger:hilt-compiler:2.57.2")

    // Hilt Navigation Compose (recommended for Compose + Hilt)
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    //extend icons
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Modern Networking Stack
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.2.1")
    implementation("com.squareup.okhttp3:logging-interceptor:5.2.1")

    // WorkManager
//    implementation("androidx.work:work-runtime-ktx:2.11.0")
//    implementation("androidx.hilt:hilt-work:1.3.0")
//    //ksp("androidx.hilt:hilt-compiler:1.3.0")


    // Local Database (Room) - for offline storage
    implementation("androidx.room:room-runtime:2.8.2")
    implementation("androidx.room:room-ktx:2.8.2")
    ksp("androidx.room:room-compiler:2.8.2")

    // Timber for logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Animation
    implementation("androidx.compose.animation:animation:1.9.4")
    implementation("androidx.compose.animation:animation-graphics:1.9.4")


    // File operations
    implementation("commons-io:commons-io:2.15.1")

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // YouTube DL for Android (Java wrapper)
    implementation("com.github.yausername.youtubedl-android:library:0.13.0")
    implementation("com.github.yausername.youtubedl-android:ffmpeg:0.13.0")
}