@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("jvm-base-convention-module")
    id("org.jetbrains.dokka")
}

description = "CodeOwners Kotlin Library"

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    js(IR) { browser(); nodejs() }
    applyDefaultHierarchyTemplate()
    abiValidation.enabled = true
}

dependencies {
    commonMainImplementation(libs.kotlin.reflect)
    commonTestImplementation(libs.kotlin.test)
    commonTestImplementation(platform(libs.junit.bom))
}

tasks.check {
    dependsOn(tasks.checkLegacyAbi)
}
