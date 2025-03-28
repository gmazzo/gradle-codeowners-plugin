plugins {
    `kotlin-dsl`
    alias(libs.plugins.buildConfig)
}

repositories {
    gradlePluginPortal()
}

fun plugin(dep: Provider<PluginDependency>) = dep.map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

fun plugin(dep: ProviderConvertible<PluginDependency>) =
    plugin(dep.asProvider())

dependencies {
    implementation(plugin(libs.plugins.buildConfig))
    implementation(plugin(libs.plugins.dokka))
    implementation(plugin(libs.plugins.dokka.javadoc))
    implementation(plugin(libs.plugins.kotlin.jvm))
    implementation(plugin(libs.plugins.kotlin.samWithReceiver))
    implementation(plugin(libs.plugins.mavenPublish))
    implementation(plugin(libs.plugins.gradle.pluginPublish))
}

buildConfig {
    packageName = ""
    buildConfigField("KOTLIN_PLUGIN", plugin(libs.plugins.kotlin.jvm))
    buildConfigField("ANDROID_PLUGIN", plugin(libs.plugins.android.application))
}
