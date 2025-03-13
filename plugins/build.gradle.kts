plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    id("jvm-convention-module") apply false
    alias(libs.plugins.axion.release)
    alias(libs.plugins.publicationsReport)
    base
}

allprojects {
    group = "io.github.gmazzo.codeowners"
    version = rootProject.scmVersion.version
}

tasks.build {
    subprojects { dependsOn(tasks.build) }
}

tasks.check {
    subprojects { dependsOn(tasks.check) }
}

tasks.register("publish") {
    subprojects { dependsOn(tasks.named("publish")) }
}

tasks.register("publishToMavenLocal") {
    subprojects { dependsOn(tasks.named("publishToMavenLocal")) }
}
