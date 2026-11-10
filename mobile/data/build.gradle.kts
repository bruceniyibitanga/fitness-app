/*
 * :data is the data layer both apps share: repositories and, from the next
 * step of Phase 1, the encrypted database they read from.
 *
 * It exposes :core and coroutines as `api` dependencies because its public
 * functions take and return :core types and Flows.
 */
plugins {
    alias(libs.plugins.levitx.android.library)
    alias(libs.plugins.levitx.hilt)
}

android {
    namespace = "app.levitx.data"
}

dependencies {
    api(project(":core"))
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
