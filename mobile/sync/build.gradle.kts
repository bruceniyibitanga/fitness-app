/*
 * :sync keeps the phone and the watch in step over the Wearable Data Layer.
 *
 * Both apps use it. The phone publishes what the watch needs to run a session
 * (the goal, bests and bands) and takes in finished sessions; the watch does
 * the reverse. See docs/app/adr/0003-phone-watch-communication.md.
 *
 * The rules (what is sent, how it is checked, when it is confirmed) are plain
 * Kotlin behind a small interface, so they are unit tested on the JVM without
 * a phone or a watch.
 */
plugins {
    alias(libs.plugins.levitx.android.library)
    alias(libs.plugins.levitx.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.levitx.sync"
}

dependencies {
    implementation(project(":data"))

    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(project(":testing"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
