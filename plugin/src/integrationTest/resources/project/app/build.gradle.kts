plugins {
    id("com.android.application")
    id("com.github.gmazzo.codeowners")
}

android {
    namespace = "org.test.app"
    compileSdk = 30
}

dependencies {
    implementation(project(":lib"))
}
