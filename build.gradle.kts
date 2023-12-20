plugins {
    base
    `maven-publish`
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.publicationsReport)
}

val pluginsBuild = gradle.includedBuild("plugins")

tasks.build {
    dependsOn(pluginsBuild.task(":$name"))
}

tasks.check {
    dependsOn(pluginsBuild.task(":$name"))
}

tasks.publish {
    dependsOn(pluginsBuild.task(":$name"))
}

tasks.publishToMavenLocal {
    dependsOn(pluginsBuild.task(":$name"))
}
