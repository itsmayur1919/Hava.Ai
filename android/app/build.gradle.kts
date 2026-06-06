plugins {
  id("com.android.application")
  kotlin("android")
}

android {
  namespace = "com.astrasoft.havaman"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.astrasoft.havaman"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"

    vectorDrawables {
      useSupportLibrary = true
    }
  }

  buildTypes {
    debug {
      // Use a different applicationId for debug builds so the APK can be
      // installed alongside an existing release build signed with another key.
      applicationIdSuffix = ".debug"
    }

    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.14"
  }

  packaging {
    resources {
      excludes += "META-INF/*"
    }
  }
}

dependencies {
  implementation("androidx.core:core-ktx:1.12.0")
  implementation("androidx.activity:activity-compose:1.9.0")
  implementation("androidx.compose.ui:ui:1.6.0")
  implementation("androidx.compose.material3:material3:1.2.0")
  implementation("com.google.android.material:material:1.11.0")
  implementation("androidx.compose.ui:ui-tooling-preview:1.6.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
  implementation("com.squareup.retrofit2:retrofit:2.9.0")
  implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
  implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
  // Google Play services for Auth and Location
  implementation("com.google.android.gms:play-services-auth:20.7.0")
  implementation("com.google.android.gms:play-services-location:21.0.1")
  debugImplementation("androidx.compose.ui:ui-tooling:1.6.0")
}
