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
        .withArguments(args.toList() + "-s" + "--scan")
        .withProjectDir(buildDir)
        .apply {
            withEnvironment(
                mapOf(
                    "ANDROID_HOME" to System.getenv("ANDROID_HOME"),
                    "pluginsClasspath" to pluginClasspath.joinToString(separator = File.pathSeparator)
                )
            )
        }
        .build()

    var androidTestPasses = false
    var androidBuildPasses = false
    var lib1TestPasses = false
    var lib1BuildPasses = false
    var lib2TestPasses = false
    var lib2BuildPasses = false
    val projectsBuildsPasses get() = lib1BuildPasses && lib2BuildPasses && androidBuildPasses

    @Test
    @Order(0)
    fun `lib1 tests passes`() {
        val build = runBuild(":lib1:test")

        assertEquals(TaskOutcome.SUCCESS, build.task(":lib1:generateCodeOwnersResources")?.outcome)
        assertEquals(null, build.task(":lib1:generateTestCodeOwnersResources")?.outcome)
        assertCodeOwners(
            project = "lib1", sourceSet = "main", expectedContent = """
                org/test/lib/       kotlin-devs
                org/test/utils/     kotlin-devs
            """.trimIndent()
        )
        assertCodeOwners(project = "lib1", sourceSet = "test", expectedContent = null)

        lib1TestPasses = true
    }

    @Test
    @Order(2)
    @EnabledIf("getLib1TestPasses")
    fun `lib1 builds successfully`() {
        val build = runBuild(":lib1:build")

        assertEquals(TaskOutcome.UP_TO_DATE, build.task(":lib1:generateCodeOwnersResources")?.outcome)
        assertEquals(null, build.task(":lib1:generateTestCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.NO_SOURCE, build.task(":lib1:generateIntegrationTestCodeOwnersResources")?.outcome)

        lib1BuildPasses = true
    }

    @Test
    @Order(1)
    fun `lib2 tests passes`() {
        val build = runBuild(":lib2:test")

        assertEquals(TaskOutcome.NO_SOURCE, build.task(":lib2:generateDebugCodeOwnersResources")?.outcome)
        assertEquals(null, build.task(":lib2:generateDebugUnitTestCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.NO_SOURCE, build.task(":lib2:generateReleaseCodeOwnersResources")?.outcome)
        assertEquals(null, build.task(":lib2:generateReleaseUnitTestCodeOwnersResources")?.outcome)
        assertCodeOwners(project = "lib2", sourceSet = "main", expectedContent = null)
        assertCodeOwners(project = "lib2", sourceSet = "test", expectedContent = null)

        lib2TestPasses = true
    }

    @Test
    @Order(2)
    @EnabledIf("getLib2TestPasses")
    fun `lib2 builds successfully`() {
        val build = runBuild(":lib2:build", ":lib2:packageDebugAndroidTest")

        assertEquals(TaskOutcome.NO_SOURCE, build.task(":lib2:generateDebugCodeOwnersResources")?.outcome)
        assertEquals(null, build.task(":lib2:generateDebugUnitTestCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.NO_SOURCE, build.task(":lib2:generateDebugAndroidTestCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.NO_SOURCE, build.task(":lib2:generateReleaseCodeOwnersResources")?.outcome)
        assertEquals(null, build.task(":lib2:generateReleaseUnitTestCodeOwnersResources")?.outcome)

        lib2BuildPasses = true
    }

    @Test
    @Order(3)
    fun `app tests passes`() {
        val build = runBuild(":app:test", ":app:packageDebugAndroidTest")

        assertEquals(TaskOutcome.SUCCESS, build.task(":app:generateDebugCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, build.task(":app:generateDebugUnitTestCodeOwnersResources")?.outcome)
        assertEquals(null, build.task(":app:generateReleaseCodeOwnersResources")?.outcome)
        assertEquals(null, build.task(":app:generateReleaseUnitTestCodeOwnersResources")?.outcome)
        assertCodeOwners(project = "app", sourceSet = "debug", expectedContent = """
            org/test/app/       android-devs
            org/test/utils/     android-devs
        """.trimIndent())
        assertCodeOwners(project = "app", sourceSet = "debugUnitTest", expectedContent = """
            org/test/app/       android-devs
        """.trimIndent())

        androidTestPasses = true
    }

    @Test
    @Order(4)
    @EnabledIf("getAndroidTestPasses")
    fun `app builds successfully`() {
        val build = runBuild(":app:build", ":app:packageDebugAndroidTest")

        assertEquals(TaskOutcome.UP_TO_DATE, build.task(":app:generateDebugCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, build.task(":app:generateDebugUnitTestCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, build.task(":app:generateDebugAndroidTestCodeOwnersResources")?.outcome)
        assertEquals(null, build.task(":app:generateReleaseCodeOwnersResources")?.outcome)
        assertEquals(null, build.task(":app:generateReleaseUnitTestCodeOwnersResources")?.outcome)

        androidBuildPasses = true
    }

    @Test
    @Order(5)
    @EnabledIf("getProjectsBuildsPasses")
    fun `whole project builds successfully`() {
        val build = runBuild("clean", "build")

        assertEquals(TaskOutcome.FROM_CACHE, build.task(":app:generateDebugCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.FROM_CACHE, build.task(":lib1:generateCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.NO_SOURCE, build.task(":lib2:generateDebugCodeOwnersResources")?.outcome)
    }

    private fun assertCodeOwners(
        project: String,
        sourceSet: String,
        expectedContent: String?,
    ) {
        val actual = File(buildDir, "$project/build/codeOwners/mappings/$sourceSet.CODEOWNERS")
            .takeIf { it.exists() }
            ?.readText()

        assertEquals(
            expectedContent?.let { "# Generated CODEOWNERS file for module `$project`, source set `$sourceSet`\n\n$it\n" },
            actual
        )
    }

}
