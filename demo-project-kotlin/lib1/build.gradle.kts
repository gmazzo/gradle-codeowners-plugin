plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.gmazzo.codeowners.kotlin")
}

val integrationTest = testing.suites.register<JvmTestSuite>("integrationTest")

kotlin {
    target.compilations.named("test") {
        codeOwners.enabled = false
    }
}

dependencies {
    api(projects.demoProjectKotlin.utils)

    testImplementation(libs.kotlin.test)
}

tasks.check {
    dependsOn(integrationTest)
}
