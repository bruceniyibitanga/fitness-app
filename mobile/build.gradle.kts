/*
 * Root build file. It declares plugin versions for the modules (without
 * applying them) and runs the code-quality checks across the whole repo:
 *
 * - Spotless with ktlint for formatting (`./gradlew spotlessCheck`).
 * - detekt for code smells and missing documentation (`./gradlew detekt`).
 *
 * Both run from here, over every module, so the rules live in one place.
 */

plugins {
    alias(libs.plugins.android.application) apply false
    // Declaring the Kotlin plugins here puts this Kotlin version on the build
    // classpath for every module, including the AGP built-in Kotlin compiler.
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        // Rule settings (code style, Compose naming) are in .editorconfig.
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
}

detekt {
    // Start from detekt's defaults and only list our changes in detekt.yml.
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt/detekt.yml"))
    source.setFrom(
        files(
            "core/src",
            "app-phone/src",
            "app-wear/src",
        ),
    )
    parallel = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        // SARIF shows findings inline on the pull request.
        sarif.required.set(true)
        html.required.set(false)
        xml.required.set(false)
        txt.required.set(false)
        md.required.set(false)
    }
}

// detekt 1.23.8 was compiled against Kotlin 2.0.21. Its own classpath must use
// that version, not the newer compiler the apps build with, or it fails to
// start. This is the fix the detekt docs give for a Kotlin version mismatch.
configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(libs.versions.detekt.kotlin.get())
        }
    }
}
