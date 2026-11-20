import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProps = Properties().apply {
    rootProject.file("app/release.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val releaseKey = localProps.getProperty("KEYSTORE_FILE")?.let { rootProject.file(it) }

android {
    namespace = "com.c0d3h01.androidtweaker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.c0d3h01.androidtweaker"
        minSdk = 26
        targetSdk = 36
        versionCode = 201
        versionName = "2.0.0"
    }

    signingConfigs {
        if (releaseKey != null) {
            create("release") {
                storeFile = releaseKey
                storePassword = localProps.getProperty("KEYSTORE_PASSWORD")
                keyAlias = localProps.getProperty("KEY_ALIAS", "androidtweaker")
                keyPassword = localProps.getProperty("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
}

val signedApk = layout.buildDirectory.file("outputs/apk/release/app-release.apk")
val stagedApk = layout.projectDirectory.file("release/androidtweaker.apk")
tasks.register("stageReleaseApk") {
    dependsOn("assembleRelease")
    doLast {
        check(releaseKey != null) { "Configure release signing in local.properties before staging an APK." }
        signedApk.get().asFile.copyTo(stagedApk.asFile, overwrite = true)
    }
}
