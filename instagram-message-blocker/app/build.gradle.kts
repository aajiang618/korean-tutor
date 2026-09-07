plugins {
    id("com.android.application")
}

android {
    namespace = "app.dmonly"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.dmonly"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // One shared key for every build so an APK from Android Studio installs over the one built by
    // tools/build-without-android-studio.sh (and vice versa). It is a debug key: never publish with it.
    signingConfigs {
        create("shared") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shared")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lint {
        abortOnError = false
    }
}

// No dependencies on purpose: the app uses only framework classes, so it also builds without Gradle.
