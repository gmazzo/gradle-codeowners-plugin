enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_PROJECT
    repositories {
        mavenCentral()
        google()
    }

    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}

rootProject.name = "plugins"

include(
    "base-plugin",
    "jvm-core",
    "jvm-plugin",
    "kotlin-compiler",
    "kotlin-core",
    "kotlin-plugin",
    "matcher",
)
