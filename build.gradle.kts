plugins {
    alias(libs.plugins.kotlin) apply false
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

    plugins.withId("java") {

        apply(plugin = "jacoco-report-aggregation")

        dependencies {
            "testImplementation"(libs.kotlin.test)
            "testImplementation"("org.junit.jupiter:junit-jupiter-params")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            workingDir(provider { temporaryDir })
        }

        tasks.withType<JacocoReport>().configureEach {
            reports.xml.required.set(true)
        }

        tasks.named("check") {
            dependsOn(tasks.withType<JacocoReport>())
        }

    }

}
