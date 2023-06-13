@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samWithReceiver)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.gradle.pluginPublish)
    `maven-central-publish`
}

description = "CodeOwners Gradle Plugin"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))

val integrationTest by testing.suites.registering(JvmTestSuite::class)
val pluginUnderTestImplementation by configurations.creating

samWithReceiver.annotation(HasImplicitReceiver::class.java.name)

dependencies {
    fun plugin(plugin: Provider<PluginDependency>) =
        plugin.map { create("${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}") }

    compileOnly(gradleKotlinDsl())
    compileOnly(plugin(libs.plugins.android))

    implementation(libs.jgit)

    testImplementation(gradleKotlinDsl())
    testRuntimeOnly(plugin(libs.plugins.kotlin.jvm))

    pluginUnderTestImplementation(projects.core)
    pluginUnderTestImplementation(plugin(libs.plugins.android))
    pluginUnderTestImplementation(plugin(libs.plugins.kotlin.jvm))
}

gradlePlugin {
    website.set("https://github.com/gmazzo/gradle-codeowners-plugin")
    vcsUrl.set("https://github.com/gmazzo/gradle-codeowners-plugin")

    plugins.create("codeOwners") {
        id = "io.github.gmazzo.codeowners"
        displayName = name
        implementationClass = "io.github.gmazzo.codeowners.CodeOwnersPlugin"
        description = "A Gradle plugin to propagate CODEOWNERS to JVM classes"
        tags.addAll("codeowners", "ownership", "attribution")
    }

    testSourceSets(sourceSets[integrationTest.name])
}

buildConfig {
    useKotlinOutput { internalVisibility = true }
    buildConfigField("String", "CORE_DEPENDENCY", projects.core.dependencyProject
        .publishing.publications.named<MavenPublication>("java").map {
            "\"${it.groupId}:${it.artifactId}:${it.version}\""
        }
    )
}

integrationTest {
    targets.all {
        testTask {
            mustRunAfter(tasks.test)

            // AGP 8 requires JDK 17 and we want to to be compatible with previous JDKs
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(17))
            })
        }
    }
}

tasks.check {
    dependsOn(integrationTest)
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
