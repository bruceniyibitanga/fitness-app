/*
 * Levitx mobile build: the shared training core, the Android phone app and
 * the Wear OS watch app.
 *
 * Repositories are declared here only, and each one is limited to the groups
 * it is meant to serve. That way a package can't be swapped in from an
 * unexpected repository (a dependency confusion attack).
 */

pluginManagement {
    // Convention plugins shared by the Android modules (levitx.android.*).
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Modules may not add their own repositories.
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
}

rootProject.name = "levitx"

include(":core")
include(":design")
include(":data")
include(":sync")
include(":testing")
include(":app-phone")
include(":app-wear")
