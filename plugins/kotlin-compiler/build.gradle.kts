plugins {
    alias(libs.plugins.kotlin.jvm)
    `compiler-arguments`
    `jvm-convention-module`
    `embedded-dependencies`
}

description = "CodeOwners Kotlin Compiler Plugin"

buildConfig.packageName = "io.github.gmazzo.codeowners.compiler"

dependencies {
    compileOnly(libs.kotlin.plugin.compiler.api)

    embedded(projects.matcher)
}
