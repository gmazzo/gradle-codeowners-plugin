plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.gmazzo.codeowners.kotlin")
}

kotlin {
    target.compilations.named("test") {
        codeOwners.enabled = false
    }
}

dependencies {
    api(projects.demoProjectKotlin.utils)

    testImplementation(libs.kotlin.test)
}
