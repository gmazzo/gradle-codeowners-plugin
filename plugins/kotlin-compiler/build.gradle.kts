plugins {
    alias(libs.plugins.kotlin.jvm)
    `compiler-arguments`
    `jvm-convention-module`
    `embedded-dependencies`
}

description = "CodeOwners Kotlin Compiler Plugin"

buildConfig {
    packageName = "io.github.gmazzo.codeowners.compiler"

    buildConfigField("EXPECTED_KOTLIN_VERSION", libs.kotlin.plugin.compiler.api.map { it.version!! })
}

dependencies {
    compileOnly(libs.kotlin.plugin.compiler.api)

    embedded(projects.matcher)
}
