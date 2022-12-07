dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}

buildCache.local.directory = file("$rootDir/.gradle/local-cache")

include(
    "app",
    "lib",
)
