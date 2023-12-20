import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    id("io.github.gmazzo.codeowners.kotlin")
}

kotlin {
    androidTarget()
    jvm()
    iosArm64()
    iosSimulatorArm64()
    js(IR) { browser(); nodejs() }
    applyDefaultHierarchyTemplate()

    sourceSets {
        val jvmCommonMain by creating

        getByName("jvmMain") { dependsOn(jvmCommonMain) }
        getByName("androidMain") { dependsOn(jvmCommonMain) }
    }
}

android {
    namespace = "org.test.app"
    compileSdk = 30
    buildFeatures.buildConfig = true

    compileOptions {
        sourceCompatibility(java.sourceCompatibility)
        targetCompatibility(java.targetCompatibility)
    }
}

dependencies {
    "jvmCommonMainImplementation"(projects.demoProject.lib1)
    "jvmCommonMainImplementation"(projects.demoProject.utils)

    "androidMainImplementation"(projects.demoProject.lib2)

    commonTestImplementation(libs.kotlin.test)
}

tasks.withType<KotlinJsIrLink>().configureEach {
    notCompatibleWithConfigurationCache("uses Task.project")
}
tasks.withType<KotlinNativeLink>().configureEach {
    notCompatibleWithConfigurationCache("uses Task.project")
}
