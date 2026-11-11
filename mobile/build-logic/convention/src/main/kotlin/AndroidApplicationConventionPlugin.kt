import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * `levitx.android.application`: an Android app with Compose.
 *
 * Applies the SDK levels, Java version, R8 release settings, lint rules and
 * Compose setup both apps share. Each app's build file then only sets its
 * namespace, application id, minimum SDK and dependencies.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<ApplicationExtension> {
                compileSdk = libs.intVersion("android-compileSdk")
                defaultConfig.targetSdk = libs.intVersion("android-targetSdk")
                compileOptions.useJava17()
                buildFeatures.compose = true
                lint.applyLevitxRules()

                val optimizeRules = getDefaultProguardFile("proguard-android-optimize.txt")
                buildTypes.getByName("release") {
                    // Shrink, optimise and obfuscate release builds with R8.
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(optimizeRules, "proguard-rules.pro")
                }
                useTestSigningWhenProvided(target)
            }

            configureKotlin()
            alignLifecycleVersion()
            addComposeDependencies()
        }
    }
}
