@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samWithReceiver)
    alias(libs.plugins.gradle.pluginPublish)
    `compiler-arguments`
    `plugin-compatibility-test`
    `maven-central-publish`
    jacoco
}

description = "CodeOwners Gradle Plugin"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))

val pluginUnderTestImplementation by configurations.creating

samWithReceiver.annotation(HasImplicitReceiver::class.java.name)

val compileOnlyWithTests by configurations.creating

configurations {
    compileOnly { extendsFrom(compileOnlyWithTests) }
    testRuntimeOnly { extendsFrom(compileOnlyWithTests) }
}

dependencies {
    fun plugin(plugin: Provider<PluginDependency>) =
        plugin.map { create("${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}") }

    implementation(projects.basePlugin)
    implementation(projects.matcher)

    testImplementation(testFixtures(projects.basePlugin))

    compileOnlyWithTests(gradleKotlinDsl())
    compileOnlyWithTests(libs.kotlin.plugin.api)
    compileOnlyWithTests(libs.kotlin.plugin.compiler.api)
    compileOnlyWithTests(plugin(libs.plugins.kotlin.jvm))

    pluginUnderTestImplementation(plugin(libs.plugins.kotlin.jvm))
}

tasks.test {
    workingDir(temporaryDir)
}

gradlePlugin {
    website.set("https://github.com/gmazzo/gradle-codeowners-plugin")
    vcsUrl.set("https://github.com/gmazzo/gradle-codeowners-plugin")

    plugins.create("codeOwnersKotlin") {
        id = "io.github.gmazzo.codeowners.kotlin"
        displayName = name
        implementationClass = "io.github.gmazzo.codeowners.CodeOwnersKotlinPlugin"
        description = "A Gradle plugin to propagate CODEOWNERS to Kotlin classes"
        tags.addAll("codeowners", "ownership", "attribution")
    }
}

buildConfig {
    fun ProjectDependency.dependencyNotation() = "${group}:${name}:${version}"

    packageName = "io.github.gmazzo.codeowners"
    buildConfigField("CORE_DEPENDENCY", projects.kotlinCore.dependencyNotation())
    buildConfigField("COMPILER_DEPENDENCY", projects.kotlinCompiler.dependencyNotation())
}

tasks.pluginUnderTestMetadata {
    pluginClasspath.from(pluginUnderTestImplementation)
}

// makes sure to publish to mavenCentral first, before doing it to Plugins Portal
tasks.publishPlugins {
    mustRunAfter(tasks.publishToSonatype)
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
