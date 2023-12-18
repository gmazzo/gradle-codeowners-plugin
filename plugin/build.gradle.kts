plugins {
    base
    `maven-publish`
    alias(libs.plugins.kotlin.jvm) apply false
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

}

tasks.build {
    subprojects { dependsOn(tasks.build) }
}

tasks.check {
    subprojects { dependsOn(tasks.check) }
}

tasks.publish {
    subprojects { dependsOn(tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)) }
}

tasks.publishToMavenLocal {
    subprojects { dependsOn(tasks.named(MavenPublishPlugin.PUBLISH_LOCAL_LIFECYCLE_TASK_NAME)) }
}
