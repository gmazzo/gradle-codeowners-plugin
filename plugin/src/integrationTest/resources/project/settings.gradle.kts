import java.io.File

val PLUGINS_CLASSPATH: String? by System.getenv().withDefault { null }
val LOCAL_REPO: String? by System.getenv().withDefault { null }

if (PLUGINS_CLASSPATH == null) {
    // meant to allow open this test project as a standalone project
    pluginManagement {
        repositories {
            mavenLocal()
            mavenCentral()
            google()
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        LOCAL_REPO?.let { maven(File(it)) }
    }
}

buildCache.local.directory = file("$rootDir/.gradle/buildCache")

include(
    "app",
    "lib1",
    "lib2",
    "utils",
)
