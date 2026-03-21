@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.importClasses)
    id("jvm-convention-module")
}

description = "CodeOwners Matcher Library"

kotlin.abiValidation.filters.excluded.byNames.addAll("io.github.gmazzo.codeowners.matcher.jgit.**")

importClasses {
    repackageTo = "io.github.gmazzo.codeowners.matcher.jgit"
    keep("org.eclipse.jgit.ignore.FastIgnoreRule")
    include("**.class")
}

dependencies {
    importClasses(libs.jgit)
    importClassesLibraries(libs.slf4j.api)
    implementation(libs.slf4j.api)

    testImplementation(libs.junit.params)
}
