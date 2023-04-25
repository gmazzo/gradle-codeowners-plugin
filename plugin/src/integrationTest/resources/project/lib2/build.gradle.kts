plugins {
    id("com.android.library")
    id("io.github.gmazzo.codeowners")
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

androidComponents.onVariants {
    it.unitTest!!.codeOwners.enabled.set(false)
}
