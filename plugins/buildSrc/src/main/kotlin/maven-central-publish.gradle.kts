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

val sonatypeRelease = rootProject.tasks.named("closeAndReleaseSonatypeStagingRepository")

sonatypeRelease {
    // TODO workaround of "Task ':kotlin-core:publishIosArm64PublicationToSonatypeRepository' uses this output of
    //  task ':kotlin-core:signIosSimulatorArm64Publication' without declaring an explicit or implicit dependency."
    mustRunAfter(tasks.withType<AbstractPublishToMaven>())
}

tasks.publish {
    dependsOn("publishToSonatype")
    mustRunAfter(sonatypeRelease)
}

// makes sure stage repository is closed and release after publishing to it
tasks.named("publishToSonatype") {
    finalizedBy(sonatypeRelease)
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
