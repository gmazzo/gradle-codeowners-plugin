plugins {
    java
    id("io.github.gmazzo.codeowners.jvm")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
}

testing.suites.withType<JvmTestSuite>().configureEach {
    useJUnitJupiter()
}
