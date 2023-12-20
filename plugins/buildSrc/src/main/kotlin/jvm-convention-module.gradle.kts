plugins {
    `java-base`
    jacoco
    id("maven-central-publish")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))

plugins.withId("java") {

    publishing {
        publications {
            create<MavenPublication>("java") { from(components["java"]) }
        }
    }

}
