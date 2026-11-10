/*
 * :testing holds fakes of the data layer's interfaces for unit tests.
 *
 * Fakes keep state in memory, so ViewModel tests run on the JVM in
 * milliseconds without a device, a database or the Keystore. Modules only
 * ever depend on this from `testImplementation`, so none of it ships.
 */
plugins {
    alias(libs.plugins.levitx.android.library)
}

android {
    namespace = "app.levitx.testing"
}

dependencies {
    api(project(":data"))
}
