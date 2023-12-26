@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samWithReceiver)
    alias(libs.plugins.gradle.pluginPublish)
    com.github.gmazzo.buildconfig
    `plugin-compatibility-test`
    `maven-central-publish`
    jacoco
}

description = "CodeOwners JVM Gradle Plugin"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))

val pluginUnderTestImplementation by configurations.creating

samWithReceiver.annotation(HasImplicitReceiver::class.java.name)

dependencies {
    fun plugin(plugin: Provider<PluginDependency>) =
        plugin.map { create("${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}") }

    compileOnly(gradleKotlinDsl())
    compileOnly(plugin(libs.plugins.android.application))

    implementation(projects.basePlugin)
    implementation(projects.matcher)

    testImplementation(gradleKotlinDsl())
    testImplementation(testFixtures(projects.basePlugin))

    testRuntimeOnly(plugin(libs.plugins.kotlin.jvm))

    pluginUnderTestImplementation(plugin(libs.plugins.android.application))
    pluginUnderTestImplementation(plugin(libs.plugins.kotlin.jvm))
}

tasks.test {
    workingDir(temporaryDir)
}

gradlePlugin {
    website.set("https://github.com/gmazzo/gradle-codeowners-plugin")
    vcsUrl.set("https://github.com/gmazzo/gradle-codeowners-plugin")

    plugins.create("codeOwnersJVM") {
        id = "io.github.gmazzo.codeowners.jvm"
        displayName = name
        implementationClass = "io.github.gmazzo.codeowners.CodeOwnersJVMPlugin"
        description = "A Gradle plugin to propagate CODEOWNERS to JVM classes"
        tags.addAll("codeowners", "ownership", "attribution")
    }
}

buildConfig {
    useKotlinOutput { internalVisibility = true }
    packageName = "io.github.gmazzo.codeowners"
    buildConfigField("CORE_DEPENDENCY", projects.jvmCore.dependencyProject
        .publishing.publications.named<MavenPublication>("java").map {
            "${it.groupId}:${it.artifactId}:${it.version}"
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
