plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-test-fixtures`
    `maven-central-publish`
    jacoco
}

description = "CodeOwners Library"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

publishing.publications {
    create<MavenPublication>("java") { from(components["java"]) }
}

// disables testFixtures artifact publication
components.named<AdhocComponentWithVariants>("java") {
    withVariantsFromConfiguration(configurations.testFixturesApiElements.get()) { skip() }
    withVariantsFromConfiguration(configurations.testFixturesRuntimeElements.get()) { skip() }
}

publishing.repositories {
    maven(layout.buildDirectory.dir("repo")) { name = "Local" }
}
