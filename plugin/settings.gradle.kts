enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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

rootProject.name = "root"

include(
    "base-plugin",
    "jvm-core",
    "jvm-plugin",
    "kotlin-compiler",
    "kotlin-core",
    "kotlin-plugin",
    "matcher",
)
