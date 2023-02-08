plugins {
    alias(libs.plugins.kotlin) apply false
    id("me.qoomon.git-versioning") version "6.3.7"
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

gitVersioning.apply {
    refs {
        branch(".+") {
            describeTagPattern = "v(?<version>.*)"
            version = "\${describe.tag.version}-SNAPSHOT"
        }
        tag("v(?<version>.*)") {
            version = "\${ref.version}"
        }
    }
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
        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }

        apply(plugin = "jacoco-report-aggregation")

        dependencies {
            "testImplementation"(libs.junit)
            "testImplementation"(libs.junit.params)
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
