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
