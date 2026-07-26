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
        versionCode = 11
        versionName = "0.20-echobackoff"
        // Debug/measurement build: everything tunable in-app; nothing baked as a release secret.
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
        /*
         * S4: there was no release buildType at all, so every APK handed out in the field was
         * a debug build with isDebuggable = true. That leaves JDWP open to `adb`, and anyone
         * with a minute of USB access can attach and read the long-term X25519 secret, the
         * per-pairing salts, every pair chain key, and message plaintext straight out of the
         * running process — defeating the encrypted-at-rest storage entirely, since all of it
         * is decrypted in memory while the service runs.
         *
         * Debug-signed on purpose: these are sideloaded field builds, and a release keystore
         * is one more secret to protect and lose. Not Play-publishable, which is fine.
         */
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
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
