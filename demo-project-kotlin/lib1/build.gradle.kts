plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.gmazzo.codeowners.kotlin")
}

val integrationTest by testing.suites.registering(JvmTestSuite::class)

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
