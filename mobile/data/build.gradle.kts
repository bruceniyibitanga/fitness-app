/*
 * :data is the data layer both apps share: the encrypted database, the key
 * that protects it, and the repositories the UI reads through.
 *
 * It exposes :core and coroutines as `api` dependencies because its public
 * functions take and return :core types and Flows.
 */
plugins {
    alias(libs.plugins.levitx.android.library)
    alias(libs.plugins.levitx.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.levitx.data"

    defaultConfig {
        // R8 rules this library needs in every app that uses it.
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

room {
    // Room writes the schema of every database version here. They are
    // committed, so migrations can be written and tested against the exact
    // schema each release shipped with.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(project(":core"))
    api(libs.kotlinx.coroutines.core)

    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.sqlcipher.android)
    // The coach's state and the workout in progress are stored as JSON documents.
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.room.testing)
}
