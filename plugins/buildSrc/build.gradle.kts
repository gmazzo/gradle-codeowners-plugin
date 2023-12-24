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

dependencies {
    implementation(plugin(libs.plugins.buildConfig))
    implementation(plugin(libs.plugins.dokka))
}

buildConfig {
    buildConfigField("KOTLIN_PLUGIN", plugin(libs.plugins.kotlin.jvm))
    buildConfigField("ANDROID_PLUGIN", plugin(libs.plugins.android.application))
}
