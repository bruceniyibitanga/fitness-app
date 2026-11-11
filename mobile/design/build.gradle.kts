/*
 * :design holds the Levitx design tokens: colours, fonts, type sizes and
 * spacing. Both apps build their Material themes from these, so the phone,
 * the watch and the website share one visual language.
 *
 * It only depends on Compose UI, not on phone or watch Material libraries,
 * so neither app pulls in the other's UI toolkit.
 */
plugins {
    alias(libs.plugins.levitx.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.levitx.design"
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
}
