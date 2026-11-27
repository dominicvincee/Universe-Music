plugins {
    id("com.android.application")
}

android {
    namespace = "com.universemusic"
    compileSdk = 36

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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

configurations.configureEach {
    resolutionStrategy.force("com.squareup.okhttp3:okhttp-android:5.4.0")
}

dependencies {
    implementation("com.github.MetrolistGroup:innertubex:v0.13.2")
    implementation("io.ktor:ktor-client-okhttp:3.6.0")
}
