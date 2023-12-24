plugins {
    base
    `maven-publish`
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.gradle.nexusPublish)
    alias(libs.plugins.publicationsReport)
    `git-versioning`
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}

allprojects {
    group = "io.github.gmazzo.codeowners"

    plugins.withId("jacoco") {
        val jacocoTasks = tasks.withType<JacocoReport>()

        jacocoTasks.configureEach {
            reports.xml.required = true
        }

        tasks.check {
            dependsOn(jacocoTasks)
        }
    }

    plugins.withId("maven-publish") {
        the<PublishingExtension>().repositories {
            maven(layout.buildDirectory.dir("repo")) { name = "Local" }
        }
    }

}

tasks.build {
    subprojects { dependsOn(tasks.build) }
}

tasks.check {
    subprojects { dependsOn(tasks.check) }
}

tasks.publish {
    subprojects { dependsOn(tasks.publish) }
}

tasks.publishToMavenLocal {
    subprojects { dependsOn(tasks.publishToMavenLocal) }
}
