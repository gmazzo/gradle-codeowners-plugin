import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    `jvm-convention-module`
}

description = "CodeOwners Library"

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

tasks.withType<KotlinJsIrLink>().configureEach {
    notCompatibleWithConfigurationCache("uses Task.project")
}
tasks.withType<KotlinNativeLink>().configureEach {
    notCompatibleWithConfigurationCache("uses Task.project")
}
