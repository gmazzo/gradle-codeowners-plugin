plugins {
    alias(libs.plugins.kotlin.jvm)
    `jvm-convention-module`
    `embedded-dependencies`
}

description = "CodeOwners Matcher Library"

dependencies {
    implementation(libs.jgit)

    testImplementation(libs.junit.params)
}
