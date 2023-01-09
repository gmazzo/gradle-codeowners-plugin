package io.github.gmazzo.codeowners

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

    private fun runBuild(vararg args: String) = GradleRunner.create()
        .forwardOutput()
        .withPluginClasspath()
        .withArguments(args.toList() + "-s")
        .withProjectDir(buildDir)
        .apply { withEnvironment(mapOf(
            "ANDROID_HOME" to System.getenv("ANDROID_HOME"),
            "pluginsClasspath" to pluginClasspath.joinToString(separator = File.pathSeparator))) }
        .build()

    var androidTestPasses = false
    var androidBuildPasses = false
    var libTestPasses = false
    var libBuildPasses = false
    val projectsBuildsPasses get() = libBuildPasses && androidBuildPasses

    @Test
    @Order(0)
    fun `lib tests passes`() {
        val build = runBuild(":lib1:test")

        assertEquals(TaskOutcome.SUCCESS, build.task(":lib1:generateCodeOwnersResources")?.outcome)

        libTestPasses = true
    }

    @Test
    @Order(2)
    @EnabledIf("getLibTestPasses")
    fun `lib builds successfully`() {
        val build = runBuild(":lib1:build")

        assertEquals(TaskOutcome.UP_TO_DATE, build.task(":lib1:generateCodeOwnersResources")?.outcome)

        libBuildPasses = true
    }

    @Test
    @Order(1)
    fun `app tests passes`() {
        val build = runBuild(":app:test")

        assertEquals(TaskOutcome.SUCCESS, build.task(":app:generateDebugCodeOwnersResources")?.outcome)

        androidTestPasses = true
    }

    @Test
    @Order(2)
    @EnabledIf("getAndroidTestPasses")
    fun `app builds successfully`() {
        val build = runBuild(":app:build")

        assertEquals(TaskOutcome.UP_TO_DATE, build.task(":app:generateDebugCodeOwnersResources")?.outcome)

        androidBuildPasses = true
    }

    @Test
    @Order(3)
    @EnabledIf("getProjectsBuildsPasses")
    fun `whole project builds successfully`() {
        val build = runBuild("clean", "build")

        assertEquals(TaskOutcome.FROM_CACHE, build.task(":app:generateDebugCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.FROM_CACHE, build.task(":lib1:generateCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.NO_SOURCE, build.task(":lib2:generateDebugCodeOwnersResources")?.outcome)
    }

}