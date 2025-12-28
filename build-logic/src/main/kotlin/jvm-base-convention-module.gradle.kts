@file:Suppress("UnstableApiUsage")

plugins {
    `java-base`
    com.vanniktech.maven.publish
    jacoco
}

val libs = the<VersionCatalogsExtension>().find("libs").get()

java.toolchain.languageVersion.set(JavaLanguageVersion.of(libs.findVersion("java").get().requiredVersion))

val originUrl = providers
    .exec { commandLine("git", "remote", "get-url", "origin") }
    .standardOutput.asText.map { it.trim() }
    .also { extensions.add("originUrl", it) }

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    pom {
        name = "${rootProject.name}-${project.name}"
        description = provider { project.description }
        url = originUrl

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
            connection = originUrl
            developerConnection = originUrl
            url = originUrl
        }
    }
}

plugins.withId("jvm-test-suite") {
    the<TestingExtension>().suites.withType<JvmTestSuite> {
        useKotlinTest(libs.findVersion("kotlin").get().requiredVersion)
        dependencies {
            implementation(platform(libs.findLibrary("junit.bom").get()))
        }
    }
}

// disables testFixtures artifact publication
plugins.withId("java-test-fixtures") {
    val testFixtures by the<SourceSetContainer>()

    afterEvaluate {
        components.withType<AdhocComponentWithVariants>().configureEach {
            listOfNotNull(
                configurations.getByName(testFixtures.apiElementsConfigurationName),
                configurations.getByName(testFixtures.runtimeElementsConfigurationName),
                configurations.findByName(testFixtures.sourcesElementsConfigurationName),
                configurations.findByName(testFixtures.javadocElementsConfigurationName),
            ).forEach { withVariantsFromConfiguration(it) { skip() } }
        }
    }
}

tasks.withType<JacocoReport>().configureEach {
    reports.xml.required = true
}

tasks.check {
    dependsOn(tasks.withType<JacocoReport>())
}
