plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.embeddedDependencies)
    `compiler-arguments`
    `jvm-convention-module`
}

description = "CodeOwners Kotlin Compiler Plugin"

buildConfig {
    packageName = "io.github.gmazzo.codeowners.compiler"

    buildConfigField("EXPECTED_KOTLIN_VERSION", libs.kotlin.plugin.compiler.api.map { it.version!! })
}

dependencies {
    compileOnly(libs.kotlin.plugin.compiler.api)

    // TODO for some strange reason, KMP sets `isTransitive = false` on the `kotlinCompilerPluginClasspathIosArm64Main` configuration
    //  which causes any dependency to be ignored, so we have bundle them all here
    //  https://scans.gradle.com/s/hdmdwh42uxmpy/dependencies?focusedDependency=WzUsNTYsNTA3LFs1LDU2LFs1MDddXV0&focusedDependencyView=dependencies_or_failure&toggled=W1s1XSxbNSwzN10sWzUsMzcsWzU1MV1dLFs1LDM3LFs1NTEsNTUyXV0sWzUsNTZdXQ
    embedded(projects.matcher) {
        exclude(group = "org.jetbrains.kotlin")
    }
}
