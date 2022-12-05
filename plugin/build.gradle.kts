plugins {
    alias(libs.plugins.kotlin)
    `java-gradle-plugin`
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation(projects.core)
    implementation(projects.parser)
    implementation(libs.jgit)

    testRuntimeOnly(plugin(libs.plugins.kotlin))
}

gradlePlugin.plugins.create("codeOwners") {
    id = "com.github.gmazzo.codeowners"
    implementationClass = "com.github.gmazzo.codeowners.CodeOwnersPlugin"
}

fun DependencyHandler.plugin(plugin: Provider<PluginDependency>) =
    plugin.map { create("${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}") }
