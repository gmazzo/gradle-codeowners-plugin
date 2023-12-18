plugins {
    id("com.android.library")
    id("io.github.gmazzo.codeowners.jvm")
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
    implementation(project(":utils"))
}

androidComponents.onVariants {
    it.unitTest!!.codeOwners.enabled.set(false)
}
