package com.github.gmazzo.codeowners

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.condition.EnabledIf
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CodeOwnersPluginITest {

    private val buildDir = File(".")
        .apply { deleteRecursively(); mkdirs() }
        .also { File(this::class.java.getResource("/project")!!.file).copyRecursively(it, overwrite = true) }

    private lateinit var buildArguments: List<String>

    private val build by lazy {
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(buildArguments)
            .withProjectDir(buildDir)
            .build()
    }

    var androidTestPasses = false
    var androidBuildPasses = false
    var libTestPasses = false
    var libBuildPasses = false
    val projectsBuildsPasses get() = libBuildPasses && androidBuildPasses

    @Test
    @Order(1)
    fun `app tests passes`() {
        buildArguments = listOf(":app:test")

        assertEquals(TaskOutcome.SUCCESS, build.task(":app:generateDebugCodeOwnersResources")?.outcome)

        androidTestPasses = true
    }

    @Test
    @Order(2)
    @EnabledIf("getAndroidTestPasses")
    fun `app builds successfully`() {
        buildArguments = listOf(":app:build")

        assertEquals(TaskOutcome.UP_TO_DATE, build.task(":app:generateDebugCodeOwnersResources")?.outcome)

        androidBuildPasses = true
    }

    @Test
    @Order(1)
    fun `lib tests passes`() {
        buildArguments = listOf(":lib:test")

        assertEquals(TaskOutcome.SUCCESS, build.task(":lib:generateCodeOwnersResources")?.outcome)

        libTestPasses = true
    }

    @Test
    @Order(2)
    @EnabledIf("getLibTestPasses")
    fun `lib builds successfully`() {
        buildArguments = listOf(":lib:build")

        assertEquals(TaskOutcome.UP_TO_DATE, build.task(":lib:generateCodeOwnersResources")?.outcome)

        libBuildPasses = true
    }

    @Test
    @Order(3)
    @EnabledIf("getProjectsBuildsPasses")
    fun `whole project builds successfully`() {
        buildArguments = listOf("build")

        assertEquals(TaskOutcome.FROM_CACHE, build.task(":app:generateDebugCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.FROM_CACHE, build.task(":lib:generateCodeOwnersResources")?.outcome)
    }

}