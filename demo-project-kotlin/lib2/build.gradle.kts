plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("io.github.gmazzo.codeowners.kotlin")
}

android {
    namespace = "org.test.kotlin.lib2"
    compileSdk = 30
    buildFeatures.buildConfig = false

    compileOptions {
        sourceCompatibility(java.sourceCompatibility)
        targetCompatibility(java.targetCompatibility)
    }
}

dependencies {
    implementation(projects.demoProjectKotlin.lib1)

    testImplementation(libs.kotlin.test)
}
