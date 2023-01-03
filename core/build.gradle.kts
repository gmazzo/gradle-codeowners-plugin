plugins {
    alias(libs.plugins.kotlin)
    `maven-central-publish`
}

description = "CodeOwners Library"

java.withSourcesJar()

publishing.publications {
    create<MavenPublication>("java") { from(components["java"]) }
}
