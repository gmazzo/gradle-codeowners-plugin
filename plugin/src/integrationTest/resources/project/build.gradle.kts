plugins {
    val PLUGINS_CLASSPATH: String? by System.getenv().withDefault { null }

    if (PLUGINS_CLASSPATH == null) {
        // meant to allow open this test project as a standalone project
        id("com.android.application") version "7.4.0" apply false
        kotlin("jvm") version "1.7.21" apply false
        id("io.github.gmazzo.codeowners") version "+"

    } else {
        id("io.github.gmazzo.codeowners")
    }
}
