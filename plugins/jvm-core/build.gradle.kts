plugins {
    alias(libs.plugins.kotlin.jvm)
    `jvm-convention-module`
    `java-test-fixtures`
    jacoco
}

description = "CodeOwners JVM Library"

// disables testFixtures artifact publication
components.named<AdhocComponentWithVariants>("java") {
    withVariantsFromConfiguration(configurations.testFixturesApiElements.get()) { skip() }
    withVariantsFromConfiguration(configurations.testFixturesRuntimeElements.get()) { skip() }
}

publishing.repositories {
    maven(layout.buildDirectory.dir("repo")) { name = "Local" }
}
