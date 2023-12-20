plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.gmazzo.codeowners.jvm")
}

val integrationTest by testing.suites.registering(JvmTestSuite::class)

sourceSets.test {
    codeOwners.enabled = false
}

dependencies {
    api(projects.demoProjectJvm.utils)

    testImplementation(libs.kotlin.test)
}

tasks.check {
    dependsOn(integrationTest)
}
