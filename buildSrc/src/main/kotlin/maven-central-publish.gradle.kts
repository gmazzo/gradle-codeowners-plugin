plugins {
    java
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

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.named<Jar>("javadocJar") {
    from(tasks.dokkaJavadoc)
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
        val origin = Runtime.getRuntime()
            .exec("git remote get-url origin")
            .inputStream
            .bufferedReader().use { it.readText().trim() }

        name.set("${rootProject.name}-${project.name}")
        description.set(project.description)
        url.set(origin)

        licenses {
            license {
                name.set("GNU v3")
                url.set("https://www.gnu.org/licenses/gpl-3.0")
            }
        }

        developers {
            developer {
                id.set("gmazzo")
                name.set(id)
                email.set("gmazzo65@gmail.com")
            }
        }

        scm {
            connection.set(origin)
            developerConnection.set(origin)
            url.set(origin)
        }

    }
}
