plugins {
    alias(libs.plugins.android.application)
    id("io.github.gmazzo.codeowners.jvm")
}

android {
    namespace = "org.test.jvm.app"
    compileSdk = libs.versions.android.sdk.get().toInt()
    buildFeatures.buildConfig = true

    compileOptions {
        sourceCompatibility(java.sourceCompatibility)
        targetCompatibility(java.targetCompatibility)
    }
}

androidComponents.beforeVariants {
    it.hostTests["UnitTest"]?.enable = true // makes sure `release.unitTests` are enabled
}

androidComponents.onVariants {
    it.codeOwners {
        enabled = it.buildType != "release"
    }
}

dependencies {
    implementation(projects.demoProjectJvm.lib1)
    implementation(projects.demoProjectJvm.lib2)

    // codeowners is disabled for release (in this buildscript)
    // we need this dependency to run the shared test on `test` folder
    releaseImplementation("io.github.gmazzo.codeowners:jvm-core")

    testImplementation(libs.kotlin.test.junit)
}
