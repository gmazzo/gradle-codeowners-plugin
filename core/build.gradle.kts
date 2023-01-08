plugins {
    alias(libs.plugins.kotlin)
    `java-test-fixtures`
    `maven-central-publish`
}

description = "CodeOwners Library"

java.withSourcesJar()

publishing.publications {
    create<MavenPublication>("java") { from(components["java"]) }
}
