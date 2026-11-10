/*
 * The Android phone app.
 *
 * Shared Android settings (SDK levels, R8, lint, Compose) come from the
 * levitx.android.application convention plugin in build-logic/.
 *
 * Shares its application id with :app-wear. Google Play and the Wearable Data
 * Layer both require the phone and watch apps to have the same package name
 * and signing key before they can talk to each other.
 */
plugins {
    alias(libs.plugins.levitx.android.application)
    alias(libs.plugins.levitx.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.levitx.phone"

    defaultConfig {
        applicationId = "app.levitx"
        minSdk = libs.versions.android.minSdk.phone.get().toInt()
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
    implementation(libs.compose.material3)

    // Navigation 3. Destinations are @Serializable keys so the back stack
    // survives the process being killed in the background.
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)

    testImplementation(project(":testing"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
