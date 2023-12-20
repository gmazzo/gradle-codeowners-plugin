plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samWithReceiver)
    alias(libs.plugins.gradle.pluginPublish)
    `maven-central-publish`
    jacoco
}

description = "CodeOwners Gradle Base Plugin"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))

samWithReceiver.annotation(HasImplicitReceiver::class.java.name)

dependencies {
    compileOnly(gradleKotlinDsl())
}

gradlePlugin {
    website.set("https://github.com/gmazzo/gradle-codeowners-plugin")
    vcsUrl.set("https://github.com/gmazzo/gradle-codeowners-plugin")

    plugins.create("codeOwners") {
        id = "io.github.gmazzo.codeowners"
        displayName = name
        implementationClass = "io.github.gmazzo.codeowners.CodeOwnersBasePlugin"
        description = "DEPRECATED: Use either `io.github.gmazzo.codeowners.kotlin` or `io.github.gmazzo.codeowners.jvm` instead"
        tags.addAll("codeowners", "ownership", "attribution")
    }
}

// makes sure to publish to mavenCentral first, before doing it to Plugins Portal
tasks.publishPlugins {
    mustRunAfter(tasks.publishToSonatype)
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
