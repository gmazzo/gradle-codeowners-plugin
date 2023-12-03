plugins {
<<<<<<<< HEAD:plugin/jvm-plugin/src/integrationTest/resources/project/lib2/build.gradle.kts
    id("com.android.library")
    id("io.github.gmazzo.codeowners.jvm")
========
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("io.github.gmazzo.codeowners.kotlin")
>>>>>>>> 91353a5 (Changed plugin id to `io.github.gmazzo.codeowners.kotlin`):demo-project/lib2/build.gradle.kts
}

android {
    namespace = "org.test.lib2"
    compileSdk = 30
    buildFeatures.buildConfig = false

    compileOptions {
        sourceCompatibility(java.sourceCompatibility)
        targetCompatibility(java.targetCompatibility)
    }
}

dependencies {
    implementation(projects.demoProject.lib1)

    testImplementation(libs.kotlin.test)
}
