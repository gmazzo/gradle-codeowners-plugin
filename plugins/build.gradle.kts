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
            stagingProfileId = "9513fc39f7e60"
        }
    }
}

allprojects {
    group = "io.github.gmazzo.codeowners"

    plugins.withId("jvm-test-suite") {
        the<TestingExtension>().suites.withType<JvmTestSuite> {
            useKotlinTest(libs.versions.kotlin)
            dependencies {
                implementation(platform(libs.junit.bom))
            }
        }
    }

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
    subprojects { dependsOn(tasks.publish) }
}

tasks.publishToMavenLocal {
    subprojects { dependsOn(tasks.publishToMavenLocal) }
}
