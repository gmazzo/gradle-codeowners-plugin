plugins {
    id("com.android.application")
    kotlin("android")
    id("com.github.gmazzo.codeowners")
}

android {
    namespace = "org.test.app"
    compileSdk = 30
}

dependencies {
    implementation(project(":lib"))
    testImplementation("junit:junit:4.13.2")
}
