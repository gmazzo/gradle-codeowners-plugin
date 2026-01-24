plugins {
    alias(libs.plugins.android.library)
    id("io.github.gmazzo.codeowners.kotlin")
}

android {
    namespace = "org.test.kotlin.lib2"
    compileSdk = libs.versions.android.sdk.get().toInt()
    buildFeatures.buildConfig = false

    compileOptions {
        sourceCompatibility(java.sourceCompatibility)
        targetCompatibility(java.targetCompatibility)
    }
}

androidComponents.beforeVariants {
    it.hostTests["UnitTest"]?.enable = true // makes sure `release.unitTests` are enabled
}

dependencies {
    implementation(projects.demoProjectKotlin.lib1)

    testImplementation(libs.kotlin.test.junit)
}
