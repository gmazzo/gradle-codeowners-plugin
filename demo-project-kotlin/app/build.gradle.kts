plugins {
    alias(libs.plugins.android.application)
    id("io.github.gmazzo.codeowners.kotlin")
}

android {
    namespace = "org.test.kotlin.app"
    compileSdk = libs.versions.android.sdk.get().toInt()
    buildFeatures.buildConfig = true

    compileOptions {
        sourceCompatibility(java.sourceCompatibility)
        targetCompatibility(java.targetCompatibility)
    }
}

dependencies {
    implementation(projects.demoProjectKotlin.appLib)
}
