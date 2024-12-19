plugins {
    alias(libs.plugins.kotlin.multiplatform)
    `jvm-convention-module`
    `maven-central-publish`
}

description = "CodeOwners Kotlin Library"

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    js(IR) { browser(); nodejs() }
    applyDefaultHierarchyTemplate()
}

dependencies {
    commonMainImplementation(libs.kotlin.reflect)
    commonTestImplementation(libs.kotlin.test)
    commonTestImplementation(platform(libs.junit.bom))
}

publishing.publications.withType<MavenPublication>().configureEach pub@{

    val javadocTask = tasks.register<Jar>("javadoc${this@pub.name.replaceFirstChar { it.uppercase() }}") {
        archiveBaseName = "${project.name}-${this@pub.name}"
        archiveClassifier = "javadoc"
        from(tasks.dokkaHtml)
    }

    artifact(javadocTask)
}
