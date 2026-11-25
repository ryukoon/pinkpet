plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ryukoon.pinkpet"
    compileSdk = 35

    defaultConfig {
        minSdk = 30
        targetSdk = 35
        versionCode = 3
        versionName = "1.2"
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

    flavorDimensions += "pet"
    productFlavors {
        create("pink") {
            dimension = "pet"
            applicationId = "com.ryukoon.pinkpet"
            manifestPlaceholders["appLabel"] = "진행해줘 펫"
        }
        create("bear") {
            dimension = "pet"
            applicationId = "com.ryukoon.bearpet"
            manifestPlaceholders["appLabel"] = "여유곰 펫"
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
