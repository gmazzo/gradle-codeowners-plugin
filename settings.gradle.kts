enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_PROJECT
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "gradle-codeowners-plugin"

includeBuild("plugin")
include(
    "demo-project",
    "demo-project:app",
    "demo-project:lib1",
    "demo-project:lib2",
    "demo-project:utils",
)
