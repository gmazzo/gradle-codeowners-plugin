plugins {
    id("com.android.application")
    kotlin("android")
    id("io.github.gmazzo.codeowners")
}

android {
    namespace = "org.test.app"
    compileSdk = 30
}

dependencies {
    implementation(project(":lib1"))
    implementation(project(":lib2"))
    testImplementation("junit:junit:4.13.2")
}
