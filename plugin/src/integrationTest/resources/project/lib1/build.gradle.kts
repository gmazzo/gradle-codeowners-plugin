plugins {
    kotlin("jvm")
    id("io.github.gmazzo.codeowners")
}

val integrationTest by testing.suites.registering(JvmTestSuite::class)

sourceSets.test {
    codeOwners.enabled.set(false)
}

dependencies {
    implementation("io.github.gmazzo.codeowners:core") // because we have `codeowners.default.dependency=false`
    api(project(":utils"))
    testImplementation("junit:junit:4.13.2")
}

tasks.check {
    dependsOn(integrationTest)
}
