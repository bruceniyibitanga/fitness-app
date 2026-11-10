/*
 * The Wear OS watch app. Compose for Wear OS, built-in Kotlin from AGP 9.
 *
 * Shares its application id with :app-phone. Google Play and the Wearable Data
 * Layer both require the phone and watch apps to have the same package name
 * and signing key before they can talk to each other.
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.levitx.wear"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.levitx"
        minSdk = libs.versions.android.minSdk.wear.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // Shrink, optimise and obfuscate release builds with R8.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        // Print every issue to the build log, not just the first one.
        textReport = true
        textOutput = file("stdout")
        // Version bumps are Dependabot's job. Lint flagging them would fail
        // the build every time a library ships, for no code problem.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
    }
}

kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling)
}
