plugins {
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
        sonatype()
    }
}

allprojects {

    group = "com.github.gmazzo.codeowners"

    plugins.withId("java") {

        the<JavaPluginExtension>().toolchain.languageVersion.set(JavaLanguageVersion.of(11))

        dependencies {
            "testImplementation"(libs.junit)
            "testImplementation"(libs.junit.params)
        }

        tasks.withType<Test> {
            useJUnitPlatform()
            workingDir(provider { temporaryDir })
        }

    }

}
