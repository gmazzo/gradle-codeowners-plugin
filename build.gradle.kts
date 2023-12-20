plugins {
    base
    `maven-publish`
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
