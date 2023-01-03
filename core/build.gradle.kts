plugins {
    alias(libs.plugins.kotlin)
    `maven-publish`
}

java.withSourcesJar()

publishing.publications {
    create<MavenPublication>("java") { from(components["java"]) }
}
