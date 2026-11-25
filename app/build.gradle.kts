plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ryukoon.pinkpet"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ryukoon.petfriends"
        manifestPlaceholders["appLabel"] = "펫 친구들"
        minSdk = 30
        targetSdk = 35
        versionCode = 4
        versionName = "2.0"
    }

    // 매번 같은 키로 서명 → 새 버전을 덮어서 설치 가능
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
