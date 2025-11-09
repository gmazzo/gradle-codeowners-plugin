@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("jvm-base-convention-module")
    org.jetbrains.kotlin.jvm
    org.jetbrains.`dokka-javadoc`
}

kotlin {
    abiValidation.enabled = true
    explicitApi()
}

tasks.check {
    dependsOn(tasks.checkLegacyAbi)
}
