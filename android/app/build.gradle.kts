plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.cockroachat.mesh"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.cockroachat.mesh"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1-debug"
        // Debug/measurement build: everything tunable in-app; nothing baked as a release secret.
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
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
}
