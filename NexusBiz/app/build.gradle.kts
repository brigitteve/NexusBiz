plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")
    id("kotlin-parcelize")
    kotlin("plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.nexusbiz.nexusbiz"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.nexusbiz.nexusbiz"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.foundation.android)
    implementation(libs.androidx.compose.foundation.android)
    implementation(libs.androidx.compose.foundation.layout.android)
    implementation(libs.androidx.compose.foundation.layout.android)
    implementation(libs.androidx.compose.foundation.layout.android)
    kapt(libs.androidx.room.compiler)
    
    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    
    // Image Loading
    implementation(libs.coil.compose)
    
    // QR Code
    implementation(libs.zxing.android.embedded)
    implementation(libs.zxing.core)
    
    // Permissions
    implementation(libs.accompanist.permissions)
    
    // Supabase
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.2.0")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.2.0")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.2.0")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Ktor HTTP Client Engine (requerido por Supabase)
    implementation("io.ktor:ktor-client-android:2.3.3")
    
    // Google Maps & Location
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}