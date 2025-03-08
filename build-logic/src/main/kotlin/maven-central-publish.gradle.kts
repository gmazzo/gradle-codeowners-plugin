plugins {
    signing
    `maven-publish`
    org.jetbrains.dokka
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project

    useInMemoryPgpKeys(signingKey, signingPassword)
    publishing.publications.configureEach(::sign)
    tasks.withType<Sign>().configureEach { enabled = signingKey != null }
}

plugins.withId("java") {
    configure<JavaPluginExtension> {
        withJavadocJar()
        withSourcesJar()
    }

    tasks.named<Jar>("javadocJar") {
        from(tasks.dokkaJavadoc)
    }
}

publishing.publications.withType<MavenPublication>().configureEach {
    setupMandatoryPOMAttributes()
}

tasks.publish {
    dependsOn("publishToSonatype")
    mustRunAfter(":closeAndReleaseSonatypeStagingRepository")
}

// makes sure stage repository is closed and release after publishing to it
tasks.named("publishToSonatype") {
    finalizedBy(":closeAndReleaseSonatypeStagingRepository")
}

fun MavenPublication.setupMandatoryPOMAttributes() {
    pom {
        val origin = providers
            .exec { commandLine("git", "remote", "get-url", "origin") }
            .standardOutput
            .asText
            .map(String::trim)

        name = "${rootProject.name}-${project.name}"
        description = provider { project.description }
        url = origin

        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/license/mit/"
            }
        }

        developers {
            developer {
                id = "gmazzo"
                name = id
                email = "gmazzo65@gmail.com"
            }
        }

        scm {
            connection = origin
            developerConnection = origin
            url = origin
        }

    }
}
