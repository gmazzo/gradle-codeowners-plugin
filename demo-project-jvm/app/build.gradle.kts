plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("io.github.gmazzo.codeowners.jvm")
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

androidComponents.onVariants {
    it.codeOwners.enabled = it.buildType != "release"
}

dependencies {
    implementation(projects.demoProjectJvm.lib1)
    implementation(projects.demoProjectJvm.lib2)

    // codeowners is disabled for release (in this buildscript)
    // we need this dependency to run the shared test on `test` folder
    releaseImplementation("io.github.gmazzo.codeowners:jvm-core")

    testImplementation(libs.kotlin.test)
}
