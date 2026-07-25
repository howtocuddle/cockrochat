plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "org.bileichat.mesh"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.bileichat.mesh"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.6-unified"
        // Debug/measurement build: everything tunable in-app; nothing baked as a release secret.
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // libmesh_core.so is dropped into src/main/jniLibs/<abi>/ by cargo-ndk (see build-android.sh).
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-service:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // UniFFI-generated Kotlin bindings use JNA at runtime.
    implementation("net.java.dev.jna:jna:5.14.0@aar")
    // EncryptedSharedPreferences for at-rest protection of pairing keys (A3).
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    // On-device QR encoding/decoding for out-of-band pairing. No key material is sent to a server.
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    // Compose UI (single unified app: messaging screen + left settings drawer).
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
