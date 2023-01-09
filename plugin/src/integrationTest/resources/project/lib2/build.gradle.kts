plugins {
    id("com.android.library")
    id("io.github.gmazzo.codeowners")
}

android {
    namespace = "org.test.lib2"
    compileSdk = 30
    buildFeatures.buildConfig = false
}
