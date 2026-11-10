import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * `levitx.android.library`: an Android library shared by the phone and watch apps.
 *
 * Libraries use the phone's minimum SDK because it is the lower of the two;
 * a library can't require more than its lowest consumer supports.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                compileSdk = libs.intVersion("android-compileSdk")
                defaultConfig.minSdk = libs.intVersion("android-minSdk-phone")
                compileOptions.useJava17()
                lint.applyLevitxRules()
            }

            configureKotlin()
            alignLifecycleVersion()
        }
    }
}
