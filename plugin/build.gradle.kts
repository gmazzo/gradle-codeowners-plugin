@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.buildConfig)
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
    implementation(libs.jgit)

    compileOnly(plugin(libs.plugins.android))

    testRuntimeOnly(plugin(libs.plugins.kotlin))

    pluginUnderTestImplementation(projects.core)
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
