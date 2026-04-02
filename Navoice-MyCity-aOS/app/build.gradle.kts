plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.navoice.mycity"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.navoice.mycity"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

// Production configuration – required properties (fail fast if missing)

        val publishableKey = project.findProperty("NAVOICE_PUBLISHABLE_KEY") as? String
            ?: error("NAVOICE_PUBLISHABLE_KEY is missing. Define it in gradle.properties or local.properties.")
        val backendUrl = project.findProperty("NAVOICE_BACKEND_BASE_URL") as? String
            ?: error("NAVOICE_BACKEND_BASE_URL is missing. Define it in gradle.properties or local.properties.")
        buildConfigField("String", "NAVOICE_PUBLISHABLE_KEY", "\"$publishableKey\"")
        buildConfigField("String", "NAVOICE_BACKEND_BASE_URL", "\"$backendUrl\"")
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
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation(files("libs/navoice-sdk-release.aar"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.moshi:moshi-adapters:1.15.0")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // Navoice SDK - Add when available from your maven/bitbucket
    // implementation("io.navoice:sdk-android:x.x.x")
}
