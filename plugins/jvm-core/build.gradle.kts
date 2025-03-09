plugins {
    alias(libs.plugins.kotlin.jvm)
    id("jvm-convention-module")
    `java-test-fixtures`
    jacoco
}

description = "CodeOwners JVM Library"
