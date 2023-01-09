@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.buildConfig)
    `java-gradle-plugin`
    `maven-central-publish`
    id("com.gradle.plugin-publish") version "1.1.0"
}

description = "CodeOwners Gradle Plugin"

testing.suites.register<JvmTestSuite>("integrationTest")

val integrationTest by sourceSets
val pluginUnderTestImplementation by configurations.creating

dependencies {
    fun plugin(plugin: Provider<PluginDependency>) =
        plugin.map { create("${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}") }

    implementation(gradleKotlinDsl())
    implementation(libs.jgit)

    compileOnly(plugin(libs.plugins.android))

    testRuntimeOnly(plugin(libs.plugins.kotlin))

    pluginUnderTestImplementation(projects.core)
    pluginUnderTestImplementation(plugin(libs.plugins.android))
    pluginUnderTestImplementation(plugin(libs.plugins.kotlin))
}

gradlePlugin {
    website.set("https://github.com/gmazzo/gradle-codeowners-plugin")
    vcsUrl.set("https://github.com/gmazzo/gradle-codeowners-plugin")

    plugins.create("codeOwners") {
        id = "com.github.gmazzo.codeowners"
        displayName = name
        implementationClass = "com.github.gmazzo.codeowners.CodeOwnersPlugin"
        description = "A Gradle plugin to propagate CODEOWNERS to JVM classes"
        tags.addAll("codeowners", "ownership", "attribution")
    }

    testSourceSets(integrationTest)
}

buildConfig {
    useKotlinOutput { internalVisibility = true }
    buildConfigField("String", "CORE_DEPENDENCY", projects.core.dependencyProject
        .publishing.publications.named<MavenPublication>("java").map {
            "\"${it.groupId}:${it.artifactId}:${it.version}\""
        }
    )
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
