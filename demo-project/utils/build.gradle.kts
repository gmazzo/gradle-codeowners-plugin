plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.gmazzo.codeowners.kotlin")
}

dependencies {
    testImplementation(libs.kotlin.test)
}
