allprojects {

    repositories {
        mavenCentral()
    }

    group = "com.github.gmazzo.codeowners"
    version = "1.0-SNAPSHOT"

    plugins.withId("java") {

        the<JavaPluginExtension>().toolchain.languageVersion.set(JavaLanguageVersion.of(11))

        dependencies {
            "testImplementation"(libs.junit)
            "testImplementation"(libs.junit.params)
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }

    }

}
