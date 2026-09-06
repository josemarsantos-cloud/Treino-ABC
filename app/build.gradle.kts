plugins {
    id("com.android.application")
}

android {
    namespace = "br.com.treinoabc"
    compileSdk = 37

    defaultConfig {
        applicationId = "br.com.treinoabc"
        minSdk = 28
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    }

dependencies {
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.health.connect:connect-client:1.2.0-alpha06")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
