plugins {
    val pluginsClasspath: String? by System.getenv().withDefault { null }

    if (pluginsClasspath == null) {
        // meant to allow open this test project as a standalone project
        id("com.android.application") version "7.3.1" apply false
        kotlin("jvm") version "1.7.21" apply false
        id("com.github.gmazzo.codeowners") version "0.0.0-SNAPSHOT"

    } else {
        id("com.github.gmazzo.codeowners")
    }
}
