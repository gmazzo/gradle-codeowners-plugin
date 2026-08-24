plugins {
    alias(libs.plugins.android.multiplatform)
    alias(libs.plugins.kotlin.multiplatform)
    id("io.github.gmazzo.codeowners.kotlin")
}

kotlin {
    android {
        namespace = "org.test.kotlin.lib3"
        compileSdk = libs.versions.android.sdk.get().toInt()
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()
    js { browser(); nodejs() }
    applyDefaultHierarchyTemplate()

    sourceSets {
        val jvmCommonMain = create("jvmCommonMain") { dependsOn(commonMain.get()) }

        getByName("jvmMain") { dependsOn(jvmCommonMain) }
        //getByName("androidMain") { dependsOn(jvmCommonMain) }
    }
}

androidComponents.beforeVariants {
    it.hostTests["UnitTest"]?.enable = true // makes sure `release.unitTests` are enabled
}

dependencies {
    "jvmCommonMainImplementation"(projects.demoProjectKotlin.lib1)
    "jvmCommonMainImplementation"(projects.demoProjectKotlin.utils)

    "androidMainImplementation"(projects.demoProjectKotlin.lib2)

    commonTestImplementation(libs.kotlin.test)
}
