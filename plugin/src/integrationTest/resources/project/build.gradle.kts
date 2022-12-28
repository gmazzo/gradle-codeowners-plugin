plugins {
    val pluginsClasspath: String? by System.getenv()

    if (pluginsClasspath == null) {
        // meant to allow open this test project as a standalone project
        id("com.android.application") version "7.3.1" apply false
        kotlin("jvm") version "1.7.21" apply false
        id("com.github.gmazzo.codeowners") version "1.0-SNAPSHOT" apply false

    } else {
        id("com.github.gmazzo.codeowners")
    }
}
