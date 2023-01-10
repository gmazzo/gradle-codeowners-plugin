plugins {
    kotlin("jvm")
    id("io.github.gmazzo.codeowners")
}

val integrationTest by testing.suites.registering(JvmTestSuite::class)

sourceSets.test {
    codeOwners.includeAsResources.set(false)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

tasks.check {
    dependsOn(integrationTest)
}
