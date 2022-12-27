plugins {
    kotlin("jvm")
    id("com.github.gmazzo.codeowners")
}

testing.suites.withType<JvmTestSuite> {
    useJUnit()
}
