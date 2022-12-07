plugins {
    id("com.android.application")
    id("com.github.gmazzo.codeowners")
}

android {
    compileSdk = 30
}

dependencies {
    implementation(project(":lib"))
}
