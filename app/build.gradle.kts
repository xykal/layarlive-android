plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val releaseKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
val releaseVersionName = (System.getenv("RELEASE_VERSION") ?: "v0.2.0").removePrefix("v")
val releaseVersionParts = releaseVersionName.split(".").mapNotNull { it.toIntOrNull() }
val releaseVersionCode = if (releaseVersionParts.size == 3) {
    releaseVersionParts[0] * 1_000_000 + releaseVersionParts[1] * 1_000 + releaseVersionParts[2]
} else {
    2_000
}

android {
    namespace = "com.arena.layarlive"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.arena.layarlive"
        minSdk = 26
        targetSdk = 35
        versionCode = releaseVersionCode
        versionName = releaseVersionName
    }

    signingConfigs {
        create("release") {
            if (!releaseKeystorePath.isNullOrBlank()) {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
