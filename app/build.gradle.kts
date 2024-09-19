plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.poe2"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.poe2"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX and Material Components
    implementation(libs.androidx.core.ktx.v1101)
    implementation(libs.androidx.appcompat)
    implementation(libs.material.v190)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.fragment.ktx)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.2.0")) // Use the latest Firebase BoM
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database.ktx)

    // Google Play Services
    implementation(libs.play.services.maps.v1810)
    implementation(libs.play.services.places.v1700)
    implementation(libs.play.services.location.v2130)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.v500alpha8)

    // Utilities
    implementation(libs.android.maps.utils)
    implementation(libs.places)
    implementation(libs.androidx.fragment)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)
}

