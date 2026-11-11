/*
 * Convention plugins for the Levitx mobile build.
 *
 * The Android and Kotlin Gradle plugins are compile-only here. At build time
 * the versions declared in the root build file's plugins block are the ones
 * that run, so there is only ever one version of each.
 */
plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.levitx.android.application.get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.levitx.android.library.get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("hilt") {
            id = libs.plugins.levitx.hilt.get().pluginId
            implementationClass = "HiltConventionPlugin"
        }
    }
}
