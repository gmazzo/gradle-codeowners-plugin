plugins {
    alias(libs.plugins.kotlin.jvm)
    id("jvm-convention-module")
    `java-test-fixtures`
    jacoco
}

description = "CodeOwners JVM Library"

// disables testFixtures artifact publication
components.named<AdhocComponentWithVariants>("java") {
    withVariantsFromConfiguration(configurations.testFixturesApiElements.get()) { skip() }
    withVariantsFromConfiguration(configurations.testFixturesRuntimeElements.get()) { skip() }
}
