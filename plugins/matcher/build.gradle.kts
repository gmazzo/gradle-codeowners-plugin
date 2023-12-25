plugins {
    alias(libs.plugins.kotlin.jvm)
    `jvm-convention-module`
    `embedded-dependencies`
}

description = "CodeOwners Kotlin Compiler Parser"

dependencies {
    implementation(libs.jgit)

    testImplementation("org.junit.jupiter:junit-jupiter-params")
}
