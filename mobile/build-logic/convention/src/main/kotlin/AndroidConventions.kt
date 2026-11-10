/*
 * Settings shared by the convention plugins. Each function configures one
 * concern, so a plugin reads as a list of the concerns it applies.
 */

import com.android.build.api.dsl.CompileOptions
import com.android.build.api.dsl.Lint
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/** The `libs` version catalog, as build scripts see it. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Reads a whole-number version, such as an SDK level, from the catalog. */
internal fun VersionCatalog.intVersion(alias: String): Int = findVersion(alias).get().requiredVersion.toInt()

/** Java 17 bytecode: what AGP 9 targets and what every module agrees on. */
internal fun CompileOptions.useJava17() {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

/** Lint fails the build on any warning. */
internal fun Lint.applyLevitxRules() {
    warningsAsErrors = true
    abortOnError = true
    // Version bumps are Dependabot's job. Lint flagging them would fail the
    // build every time a library ships, for no code problem.
    disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
}

/** Kotlin compiler warnings are errors, and bytecode matches [useJava17]. */
internal fun Project.configureKotlin() {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            allWarningsAsErrors.set(true)
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

/** The Compose libraries every Compose module needs, aligned by the BOM. */
internal fun Project.addComposeDependencies() {
    dependencies {
        val bom = libs.findLibrary("compose-bom").get()
        add("implementation", platform(bom))
        add("implementation", libs.findLibrary("compose-ui").get())
        add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
        add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
    }
}
