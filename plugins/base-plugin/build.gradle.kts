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
    fun plugin(dep: Provider<PluginDependency>) = with(dep.get()) {
        create("$pluginId:$pluginId.gradle.plugin:$version")
    }

    compileOnly(gradleKotlinDsl())
    compileOnly(plugin(libs.plugins.android.application))
    compileOnly(plugin(libs.plugins.kotlin.jvm))

    implementation(projects.matcher)
    implementation(libs.apache.bcel)

    testImplementation(gradleTestKit())
    testImplementation(libs.kotlin.test)
}

gradlePlugin {
    website.set("https://github.com/gmazzo/gradle-codeowners-plugin")
    vcsUrl.set("https://github.com/gmazzo/gradle-codeowners-plugin")

    plugins.create("codeOwners") {
        id = "io.github.gmazzo.codeowners"
        displayName = name
        implementationClass = "io.github.gmazzo.codeowners.CodeOwnersPlugin"
        description = "Computes the codeowners of the project's classes"
        tags.addAll("codeowners", "ownership", "attribution")
    }
}

tasks.test {
    workingDir(temporaryDir)
}

// makes sure to publish to mavenCentral first, before doing it to Plugins Portal
tasks.publishPlugins {
    mustRunAfter(tasks.publishToSonatype)
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
