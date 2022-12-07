@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin)
    `java-gradle-plugin`
    `maven-publish`
}

testing.suites.register<JvmTestSuite>("integrationTest")

val integrationTest by sourceSets
val pluginUnderTestImplementation by configurations.creating

dependencies {
    fun plugin(plugin: Provider<PluginDependency>) =
        plugin.map { create("${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}") }

    implementation(gradleKotlinDsl())

    implementation(projects.parser)
    implementation(libs.jgit)

    compileOnly(plugin(libs.plugins.android))

    testRuntimeOnly(plugin(libs.plugins.kotlin))

    pluginUnderTestImplementation(plugin(libs.plugins.android))
    pluginUnderTestImplementation(plugin(libs.plugins.kotlin))
}

gradlePlugin {
    testSourceSets(integrationTest)

    plugins.create("codeOwners") {
        id = "com.github.gmazzo.codeowners"
        implementationClass = "com.github.gmazzo.codeowners.CodeOwnersPlugin"
    }
}

tasks.pluginUnderTestMetadata {
    pluginClasspath.from(pluginUnderTestImplementation)
}
