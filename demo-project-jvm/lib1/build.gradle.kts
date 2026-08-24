plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.gmazzo.codeowners.jvm")
}

val integrationTest = testing.suites.register<JvmTestSuite>("integrationTest")

sourceSets.test {
    codeOwners {
        enabled = false
    }
}

dependencies {
    api(projects.demoProjectJvm.utils)

    testImplementation(libs.kotlin.test)
}

tasks.check {
    dependsOn(integrationTest)
}
