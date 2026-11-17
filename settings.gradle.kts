pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
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

rootProject.name = "Photidy"

include(
    ":app",
    ":core:model",
    ":core:data",
    ":core:designsystem",
    ":core:platform",
    ":core:localization",
    ":feature:home",
    ":feature:organizer",
    ":feature:filters",
    ":feature:review",
    ":feature:favorites",
    ":feature:search",
    ":feature:insights",
    ":feature:settings",
    ":feature:photodetail",
)
