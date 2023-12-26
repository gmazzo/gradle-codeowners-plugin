plugins {
    `maven-publish`
    org.jetbrains.dokka
}

val signingKey: String? by project
val signingPassword: String? by project

if (signingKey != null && signingPassword != null) {
    apply(plugin = "signing")

    configure<SigningExtension> {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(the<PublishingExtension>().publications)
    }

} else {
    logger.warn("Artifact signing disabled due lack of signing properties `signingKey` and `signingPassword`")
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

tasks.dokkaJavadoc {
    notCompatibleWithConfigurationCache("uses Task.project") // TODO dokka is not compatible with CC yet
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

rootProject.tasks.named("closeAndReleaseSonatypeStagingRepository") {
    mustRunAfter(tasks.withType<AbstractPublishToMaven>())
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
