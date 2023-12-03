plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.gradle.nexusPublish)
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
