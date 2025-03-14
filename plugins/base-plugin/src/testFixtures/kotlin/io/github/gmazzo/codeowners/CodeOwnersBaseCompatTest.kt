package io.github.gmazzo.codeowners

import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

abstract class CodeOwnersBaseCompatTest(
    private val pluginId: String,
) {

    @Suppress("EnumEntryName")
    enum class Kind(val kotlin: Boolean, val android: Boolean) {
        alone(false, false),
        withKotlin(true, false),
        withAndroid(false, true),
        withBoth(true, true)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Kind::class)
    open fun `plugin can be applied with given classpath`(kind: Kind) {
        val rootDir = File(kind.name)
        rootDir.deleteRecursively()
        rootDir.mkdirs()

        val plugins = when (kind) {
            Kind.alone -> "java"
            Kind.withKotlin -> "id(\"org.jetbrains.kotlin.jvm\")"
            Kind.withAndroid -> "id(\"com.android.application\")"
            Kind.withBoth -> """
                id("com.android.application")
                id("org.jetbrains.kotlin.android")
            """.trimIndent()
        }

        val buildFile = File(rootDir, "build.gradle.kts")
        buildFile.writeText(
            """
            plugins {
                $plugins
                id("$pluginId")
            }

            configurations.all {
                exclude("io.github.gmazzo.codeowners", "jvm-core")
                exclude("io.github.gmazzo.codeowners", "kotlin-core")
            }
            """.trimIndent()
        )

        if (kind.android) {
            buildFile.appendText(
                """

                android {
                    namespace = "org.test"
                    compileSdk = 30
                    defaultConfig {
                        minSdk = 21
                        targetSdk = 30
                    }
                }
                """.trimIndent()
            )

            File(rootDir, "src/main/AndroidManifest.xml")
                .apply { parentFile.mkdirs() }
                .writeText("<manifest/>")
        }

        File(rootDir, "settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                    google()
                }
            }
            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                    google()
                }
            }

            rootProject.name = "test"

            include(":app")
            """.trimIndent()
        )

        File(rootDir, "CODEOWNERS").createNewFile()

        val build = GradleRunner.create()
            .withProjectDir(rootDir)
            .withPluginClasspath("/$kind.classpath")
            .withArguments("codeOwnersReport", "-s")
            .build()

        assertEquals(
            if (kind.android) TaskOutcome.SUCCESS else TaskOutcome.NO_SOURCE,
            build.task(":codeOwnersReport")?.outcome
        )
    }

    fun GradleRunner.withPluginClasspath(fromResource: String) = apply {
        withPluginClasspath(
            javaClass.getResource(fromResource)!!.readText()
                .splitToSequence(File.pathSeparatorChar)
                .map(::File)
                .toList()
        )
    }

}
