plugins {
    `jvm-test-suite`
}

val integrationTest by the<TestingExtension>().suites.registering(JvmTestSuite::class) {
    dependencies {
        implementation(project())
    }
}

tasks.check {
    dependsOn(integrationTest)
}
