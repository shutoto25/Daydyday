plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.dokka)
    alias(libs.plugins.com.google.dagger.hilt.android)
    kotlin("android")
    kotlin("kapt")
}

android {
    namespace = "com.gmail.shu10.dev.app.data"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // ----- モジュール依存関係 ----- //
    implementation(project(":domain"))
    implementation(project(":core"))

    // ----- google ----- //
    // hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // ----- test ----- //
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}