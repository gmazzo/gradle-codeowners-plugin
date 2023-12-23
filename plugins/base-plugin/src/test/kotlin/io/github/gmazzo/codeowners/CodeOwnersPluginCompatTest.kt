package io.github.gmazzo.codeowners

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeOwnersPluginCompatTest {

    private val rootDir by lazy { File(".").apply { deleteRecursively(); mkdirs() } }

    @Test
    fun `plugin can be applied with Kotlin or Android ones in classpath`() {
        File(rootDir,"build.gradle.kts").writeText("""
            plugins {
                java
                id("io.github.gmazzo.codeowners")
            }
        """.trimIndent())

        File(rootDir,"settings.gradle.kts").writeText("""
            rootProject.name = "test"
        """.trimIndent())

        File(rootDir,"CODEOWNERS").createNewFile()

        val build = GradleRunner.create()
            .withProjectDir(rootDir)
            .withPluginClasspath()
            .withArguments("codeOwnersReport")
            .build()

        assertEquals(TaskOutcome.NO_SOURCE, build.task(":codeOwnersReport")?.outcome)
    }

}