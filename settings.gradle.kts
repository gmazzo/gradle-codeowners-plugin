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

includeBuild("plugins")
include(
    "demo-project-jvm",
    "demo-project-jvm:app",
    "demo-project-jvm:lib1",
    "demo-project-jvm:lib2",
    "demo-project-jvm:utils",
)
include(
    "demo-project-kotlin",
    "demo-project-kotlin:app",
    "demo-project-kotlin:lib1",
    "demo-project-kotlin:lib2",
    "demo-project-kotlin:utils",
)
