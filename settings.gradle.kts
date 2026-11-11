pluginManagement {
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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "android-studio-mobile"

include(":app")
include(":core:common")
include(":core:concurrency")
include(":core:fs")
include(":core:preferences")
include(":core:ui")
include(":data:project")
include(":data:template")
include(":domain:common")
include(":domain:editor")
include(":domain:filetree")
include(":domain:project")
include(":domain:template")
include(":feature:editor")
include(":feature:filetree")
include(":feature:project")
include(":terminal:bootstrap")
include(":terminal:emulator")
