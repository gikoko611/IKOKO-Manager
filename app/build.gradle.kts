plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.gikoko.ikokomanager"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gikoko.ikokomanager"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // Keep v0.1 simple: minification off. Enable R8 only after adding keep rules
            // for dexlib2/baksmali/apksig reflection-sensitive classes.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources.excludes += setOf(
            "META-INF/INDEX.LIST",
            "META-INF/DEPENDENCIES",
            "META-INF/NOTICE",
            "META-INF/LICENSE"
        )
    }
}

dependencies {
    // smali/baksmali/dexlib2 need java.nio etc. below API 26
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // DEX <-> smali assembler/disassembler (BSD-3-Clause, https://github.com/google/smali)
    implementation("com.android.tools.smali:smali:3.0.10")
    implementation("com.android.tools.smali:smali-baksmali:3.0.10")
    implementation("com.android.tools.smali:smali-dexlib2:3.0.10")
    implementation("com.android.tools.smali:smali-util:3.0.10")

    // APK signing v1/v2/v3 (Apache-2.0)
    implementation("com.android.tools.build:apksig:8.13.2")
}
