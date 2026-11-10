/*
 * The Wear OS watch app, in Compose for Wear OS.
 *
 * Shared Android settings (SDK levels, R8, lint, Compose) come from the
 * levitx.android.application convention plugin in build-logic/.
 *
 * Shares its application id with :app-phone. Google Play and the Wearable Data
 * Layer both require the phone and watch apps to have the same package name
 * and signing key before they can talk to each other.
 */
plugins {
    alias(libs.plugins.levitx.android.application)
    alias(libs.plugins.levitx.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.levitx.wear"

    defaultConfig {
        applicationId = "app.levitx"
        minSdk = libs.versions.android.minSdk.wear.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(project(":design"))
    implementation(project(":data"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.ui.tooling)

    // Navigation 3 with Wear OS swipe-to-dismiss.
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.wear.compose.navigation3)
    implementation(libs.kotlinx.serialization.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
