import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    `jvm-convention-module`
    `maven-central-publish`
}

description = "CodeOwners Kotlin Library"

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    js(IR) { browser(); nodejs() }
    applyDefaultHierarchyTemplate()
}

dependencies {
    commonMainImplementation(libs.kotlin.reflect)
    commonTestImplementation(libs.kotlin.test)
    commonTestImplementation(platform(libs.junit.bom))
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

kotlin.targets.configureEach target@{
    mavenPublication {
        artifact(javadocJar)
    }
}

tasks.withType<DokkaTask>().configureEach {
    notCompatibleWithConfigurationCache("uses Task.project")
}
tasks.withType<KotlinJsIrLink>().configureEach {
    notCompatibleWithConfigurationCache("uses Task.project")
}
tasks.withType<KotlinNativeLink>().configureEach {
    notCompatibleWithConfigurationCache("uses Task.project")
}
