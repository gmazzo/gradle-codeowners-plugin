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

// disables testFixtures artifact publication
components.named<AdhocComponentWithVariants>("java") {
    withVariantsFromConfiguration(configurations.testFixturesApiElements.get()) { skip() }
    withVariantsFromConfiguration(configurations.testFixturesRuntimeElements.get()) { skip() }
}
