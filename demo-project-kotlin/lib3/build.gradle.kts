import com.android.build.api.dsl.androidLibrary
import org.gradle.kotlin.dsl.kotlin

plugins {
    alias(libs.plugins.android.multiplatform)
    alias(libs.plugins.kotlin.multiplatform)
    id("io.github.gmazzo.codeowners.kotlin")
}

kotlin {
    androidLibrary {
        namespace = "org.test.kotlin.lib3"
        compileSdk = 30
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()
    js(IR) { browser(); nodejs() }
    applyDefaultHierarchyTemplate()

    sourceSets {
        val jvmCommonMain by creating { dependsOn(commonMain.get()) }

        getByName("jvmMain") { dependsOn(jvmCommonMain) }
        //getByName("androidMain") { dependsOn(jvmCommonMain) }
    }
}

dependencies {
    "jvmCommonMainImplementation"(projects.demoProjectKotlin.lib1)
    "jvmCommonMainImplementation"(projects.demoProjectKotlin.utils)

    "androidMainImplementation"(projects.demoProjectKotlin.lib2)

    commonTestImplementation(libs.kotlin.test)
}
