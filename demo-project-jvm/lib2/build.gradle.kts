plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("io.github.gmazzo.codeowners.jvm")
}

android {
    namespace = "org.test.jvm.lib2"
    compileSdk = 30
    buildFeatures.buildConfig = false

    compileOptions {
        sourceCompatibility(java.sourceCompatibility)
        targetCompatibility(java.targetCompatibility)
    }
}

dependencies {
    implementation(projects.demoProjectJvm.lib1)

    testImplementation(libs.kotlin.test)
}
