@file:Suppress("UnstableApiUsage")

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.serialization") version "1.7.20"
  id("kotlin-kapt")
  id("dagger.hilt.android.plugin")
}

android {

  namespace = "com.github.whitescent"
  compileSdk = 33

  defaultConfig {
    applicationId = "com.github.whitescent.mastify"
    minSdk = 21
    targetSdk = 33
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }
  }
  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }
  kotlinOptions {
    jvmTarget = "1.8"
  }
  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
  }
  buildFeatures {
    compose = true
  }
  packagingOptions {
    resources {
      excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
  }
}

dependencies {

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.material)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.activity.compose)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  implementation(libs.accompanist.systemuicontroller)

  implementation(libs.com.google.dagger.hilt.android)
  implementation(libs.androidx.hilt.navigation.compose)
  kapt(libs.com.google.dagger.hilt.compiler)

  implementation(libs.voyager.navigator)
  implementation(libs.voyager.hilt)
  implementation(libs.voyager.tab.navigator)

}