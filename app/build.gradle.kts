plugins {
    id("com.android.application") version "8.10.1"
    id("org.jetbrains.kotlin.android") version "1.9.24"
}

android {
    namespace = "com.example.foodanalyzer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.foodanalyzer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("io.coil-kt:coil-compose:2.7.0") // Add Coil for SVG support
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // CameraX dependencies
    implementation ("androidx.camera:camera-core:1.3.4")
    implementation ("androidx.camera:camera-camera2:1.3.4")
    implementation ("androidx.camera:camera-lifecycle:1.3.4")
    implementation ("androidx.camera:camera-view:1.3.4")
    // ML Kit Text Recognition
    implementation ("com.google.mlkit:text-recognition:16.0.0")
    // uCrop for image cropping
    implementation ("com.github.yalantis:ucrop:2.2.8")
}