import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
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

    targets.named<KotlinAndroidTarget>("android") {
        compilations.configureEach {
            codeOwners.enabled = androidVariant.buildType.name != "release"
        }
    }
}

android {
    namespace = "org.test.kotlin.app"
    compileSdk = 30
    buildFeatures.buildConfig = true

    compileOptions {
        sourceCompatibility(java.sourceCompatibility)
        targetCompatibility(java.targetCompatibility)
    }
}

dependencies {
    "jvmCommonMainImplementation"(projects.demoProjectKotlin.lib1)
    "jvmCommonMainImplementation"(projects.demoProjectKotlin.utils)

    "androidMainImplementation"(projects.demoProjectKotlin.lib2)

    commonTestImplementation(libs.kotlin.test)
}

tasks.withType<KotlinJsIrLink>().configureEach {
    notCompatibleWithConfigurationCache("uses Task.project")
}
tasks.withType<KotlinNativeLink>().configureEach {
    notCompatibleWithConfigurationCache("uses Task.project")
}
