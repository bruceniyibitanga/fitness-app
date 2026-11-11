/*
 * build-logic is an included build holding the Gradle convention plugins.
 * Each plugin applies the settings every module of one kind shares, so the
 * module build files stay short and can't drift apart.
 *
 * It reads the same version catalog as the main build.
 */

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
