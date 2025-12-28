plugins {
    alias(libs.plugins.android.application)
    id("io.github.gmazzo.codeowners.kotlin")
}

android {
    namespace = "org.test.kotlin.app"
    compileSdk = 30
    buildFeatures.buildConfig = true
}

dependencies {
    implementation(projects.demoProjectKotlin.lib1)
    implementation(projects.demoProjectKotlin.utils)
    implementation(projects.demoProjectKotlin.lib2)
    implementation(projects.demoProjectKotlin.lib3)
    testImplementation(libs.kotlin.test)
}
