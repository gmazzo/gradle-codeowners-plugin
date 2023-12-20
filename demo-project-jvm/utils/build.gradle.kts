plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.gmazzo.codeowners.jvm")
}

dependencies {
    testImplementation(libs.kotlin.test)
}
