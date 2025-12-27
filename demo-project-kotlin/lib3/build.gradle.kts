import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

plugins {
    alias(libs.plugins.android.multiplatform)
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
        val jvmCommonMain by creating { dependsOn(commonMain.get()) }

        getByName("jvmMain") { dependsOn(jvmCommonMain) }
        getByName("androidMain") { dependsOn(jvmCommonMain) }
    }

    targets.named<KotlinAndroidTarget>("android") {
        compilations.configureEach {
            codeOwners.enabled = "release" !in name.lowercase()
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
