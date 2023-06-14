import java.io.File

val pluginsClasspath: String? by System.getenv().withDefault { null }

if (pluginsClasspath == null) {
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

        // allows resolution of the local core dependency added by the plugin
        pluginsClasspath?.split(File.pathSeparatorChar)?.let { paths ->
            flatDir {
                dir(paths.map { file(it).parentFile })
            }
        }
    }
}

buildCache.local.directory = file("$rootDir/.gradle/buildCache")

include(
    "app",
    "lib1",
    "lib2",
    "utils",
)
