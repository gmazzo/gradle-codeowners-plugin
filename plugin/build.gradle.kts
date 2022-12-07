plugins {
    alias(libs.plugins.kotlin)
    `java-gradle-plugin`
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation(projects.parser)
    implementation(libs.jgit)

    compileOnly(plugin(libs.plugins.android))

    testRuntimeOnly(plugin(libs.plugins.kotlin))
}

gradlePlugin.plugins.create("codeOwners") {
    id = "com.github.gmazzo.codeowners"
    implementationClass = "com.github.gmazzo.codeowners.CodeOwnersPlugin"
}

fun DependencyHandler.plugin(plugin: Provider<PluginDependency>) =
    plugin.map { create("${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}") }
