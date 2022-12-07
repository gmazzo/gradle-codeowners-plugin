plugins {
    alias(libs.plugins.kotlin)
    `maven-publish`
}

publishing.publications {
    create<MavenPublication>("java") { from(components["java"]) }
}
