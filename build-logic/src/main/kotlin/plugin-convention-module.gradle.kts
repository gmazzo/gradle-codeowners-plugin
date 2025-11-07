plugins {
    org.jetbrains.kotlin.jvm
    org.jetbrains.kotlin.plugin.sam.with.receiver
    id("jvm-convention-module")
    id("plugin-compatibility-test")
    com.gradle.`plugin-publish`
}

samWithReceiver.annotation(HasImplicitReceiver::class.java.name)

val originUrl = extensions.getByName<Provider<String>>("originUrl")

gradlePlugin {
    website = originUrl
    vcsUrl = originUrl
}

afterEvaluate {
    tasks.named<Jar>("javadocJar") {
        from(tasks.dokkaGeneratePublicationJavadoc)
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    mustRunAfter(tasks.publishPlugins)
}

tasks.validatePlugins {
    enableStricterValidation = true
}

tasks.publishPlugins {
    enabled = "$version".matches("\\d+(\\.\\d+)+".toRegex())
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
