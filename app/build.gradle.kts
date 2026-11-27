plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.universemusic"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.universemusic"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.github.MetrolistGroup.innertubex:innertubex:v0.13.1")
    implementation("io.ktor:ktor-client-okhttp:3.6.0")
}
