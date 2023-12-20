@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samWithReceiver)
    alias(libs.plugins.gradle.pluginPublish)
    com.github.gmazzo.buildconfig
    `java-integration-tests`
    `maven-central-publish`
    jacoco
}

description = "CodeOwners Gradle Plugin"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))

val pluginUnderTestImplementation by configurations.creating

samWithReceiver.annotation(HasImplicitReceiver::class.java.name)

dependencies {
    fun plugin(plugin: Provider<PluginDependency>) =
        plugin.map { create("${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}") }

    compileOnly(gradleKotlinDsl())
    compileOnly(plugin(libs.plugins.android.application))

    implementation(projects.basePlugin)
    implementation(projects.matcher)

    testImplementation(gradleKotlinDsl())
    testRuntimeOnly(plugin(libs.plugins.kotlin.jvm))

    pluginUnderTestImplementation(plugin(libs.plugins.android.application))
    pluginUnderTestImplementation(plugin(libs.plugins.kotlin.jvm))
}

testing.suites.withType<JvmTestSuite> {
    useKotlinTest(libs.versions.kotlin)
    dependencies {
        implementation(platform(libs.junit.bom))
    }
    targets.all {
        testTask {
            workingDir(provider { temporaryDir })
        }
    }
}

gradlePlugin {
    website.set("https://github.com/gmazzo/gradle-codeowners-plugin")
    vcsUrl.set("https://github.com/gmazzo/gradle-codeowners-plugin")

    plugins.create("codeOwnersJVM") {
        id = "io.github.gmazzo.codeowners.jvm"
        displayName = name
        implementationClass = "io.github.gmazzo.codeowners.CodeOwnersJVMPlugin"
        description = "A Gradle plugin to propagate CODEOWNERS to JVM classes"
        tags.addAll("codeowners", "ownership", "attribution")
    }

    testSourceSets(sourceSets["integrationTest"])
}

buildConfig {
    useKotlinOutput { internalVisibility = true }
    packageName = "io.github.gmazzo.codeowners"
    buildConfigField("String", "CORE_DEPENDENCY", projects.jvmCore.dependencyProject
        .publishing.publications.named<MavenPublication>("java").map {
            "\"${it.groupId}:${it.artifactId}:${it.version}\""
        }
    )
}

tasks.integrationTest {
    shouldRunAfter(tasks.test)

    val core = projects.jvmCore.dependencyProject
    dependsOn("${core.path}:publishAllPublicationsToLocalRepository")
    environment("LOCAL_REPO", core.layout.buildDirectory.dir("repo").get().asFile.toRelativeString(workingDir))

    // AGP 8 requires JDK 17, and we want to be compatible with previous JDKs
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    })
}

tasks.pluginUnderTestMetadata {
    pluginClasspath.from(pluginUnderTestImplementation)
}

// makes sure to publish to mavenCentral first, before doing it to Plugins Portal
tasks.publishPlugins {
    mustRunAfter(tasks.publishToSonatype)
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
