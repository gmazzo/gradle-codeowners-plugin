@file:Suppress("UnstableApiUsage")

import org.gradle.plugin.compatibility.compatibility


plugins {
    id("plugin-convention-module")
    id("com.github.gmazzo.buildconfig")
}

description = "A Gradle plugin to propagate CODEOWNERS to JVM classes"

val pluginUnderTestImplementation = configurations.create("pluginUnderTestImplementation")

dependencies {
    fun plugin(plugin: Provider<PluginDependency>) =
        plugin.map { create("${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}") }

    compileOnly(gradleKotlinDsl())
    compileOnly(plugin(libs.plugins.android.application))

    api(projects.basePlugin)
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
        description = project.description
        compatibility {
            features {
                configurationCache = true
                isolatedProjects = true
            }
        }
        tags.addAll("codeowners", "ownership", "attribution")
    }
}

buildConfig {
    useKotlinOutput { internalVisibility = true }
    packageName = "io.github.gmazzo.codeowners"

    buildConfigField(
        "CORE_DEPENDENCY", projects.jvmCore
            .let { "${it.group}:${it.name}:${it.version}" }
    )
}

tasks.pluginUnderTestMetadata {
    pluginClasspath.from(pluginUnderTestImplementation)
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
