plugins {
    id("com.android.application")
    kotlin("android")
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
    it.codeOwners.enabled.set(it.buildType != "release")
}

dependencies {
    implementation(project(":lib1"))
    implementation(project(":lib2"))
    testReleaseImplementation("io.github.gmazzo.codeowners:jvm-core") // required to the test to compile
    testImplementation("junit:junit:4.13.2")
}
