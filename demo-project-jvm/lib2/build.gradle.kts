plugins {
    alias(libs.plugins.android.library)
    id("io.github.gmazzo.codeowners.jvm")
}

android {
    namespace = "org.test.jvm.lib2"
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
    implementation(projects.demoProjectJvm.lib1)

    testImplementation(libs.kotlin.test.junit)
}
