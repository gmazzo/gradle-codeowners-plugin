@file:Suppress("UnstableApiUsage")

plugins {
    id("plugin-convention-module")
    id("com.github.gmazzo.buildconfig")
}

description = "A Gradle plugin to propagate CODEOWNERS to JVM classes"

val pluginUnderTestImplementation by configurations.creating

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
        description = project.description
        tags.addAll("codeowners", "ownership", "attribution")
    }
}

buildConfig {
    useKotlinOutput { internalVisibility = true }
    packageName = "io.github.gmazzo.codeowners"

    buildConfigField(
        "CORE_DEPENDENCY", projects.jvmCore.dependencyProject
            .publishing.publications.named<MavenPublication>("maven")
            .map { "${it.groupId}:${it.artifactId}:${it.version}" }
    )
}

tasks.pluginUnderTestMetadata {
    pluginClasspath.from(pluginUnderTestImplementation)
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
