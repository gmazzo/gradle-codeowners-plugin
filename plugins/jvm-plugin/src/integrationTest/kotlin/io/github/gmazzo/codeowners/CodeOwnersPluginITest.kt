package io.github.gmazzo.codeowners

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
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
        .apply {
            withEnvironment(
                mapOf(
                    "ANDROID_HOME" to System.getenv("ANDROID_HOME"),
                    "PLUGINS_CLASSPATH" to pluginClasspath.joinToString(separator = File.pathSeparator),
                    "LOCAL_REPO" to System.getenv("LOCAL_REPO"),
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
    var utilsTestPasses = false
    val projectsBuildsPasses get() = utilsTestPasses && lib1BuildPasses && lib2BuildPasses && androidBuildPasses

    @Test
    @Order(0)
    fun `utils tests passes`() {
        val build = runBuild(":utils:test")

        assertEquals(TaskOutcome.SUCCESS, build.task(":utils:generateCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, build.task(":utils:generateTestCodeOwnersResources")?.outcome)

        val simplifiedCodeOwners = """
            org/test/utils/     utils-devs
        """.trimIndent()

        val rawCodeOwners = """
            org/test/utils/                     utils-devs
            org/test/utils/Utils                utils-devs
            org/test/utils/more/                utils-devs
            org/test/utils/more/MoreUtils       utils-devs
        """.trimIndent()

        val rawTestCodeOwners = """
            org/test/utils/                     utils-devs
            org/test/utils/UtilsOwnersTest      utils-devs
        """.trimIndent()

        assertCodeOwners("utils", "main", simplifiedCodeOwners)
        assertCodeOwners("utils", "main", rawCodeOwners, simplified = false)
        assertCodeOwners("utils", "test", simplifiedCodeOwners)
        assertCodeOwners("utils", "test", rawTestCodeOwners, simplified = false)

        utilsTestPasses = true
    }

    @Test
    @Order(1)
    @EnabledIf("getUtilsTestPasses")
    fun `utils builds successfully`() {
        val build = runBuild(":utils:build")

        assertEquals(TaskOutcome.UP_TO_DATE, build.task(":utils:generateCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, build.task(":utils:generateTestCodeOwnersResources")?.outcome)
    }

    @Test
    @Order(2)
    fun `lib1 tests passes`() {
        val build = runBuild(":lib1:test")

        assertEquals(TaskOutcome.SUCCESS, build.task(":lib1:generateCodeOwnersResources")?.outcome)
        assertEquals(null, build.task(":lib1:generateTestCodeOwnersResources")?.outcome)

        val simplifiedCodeOwners = """
            org/test/lib/               libs-devs
            org/test/utils/             libs-devs utils-devs
            org/test/utils/LibUtils     libs-devs
            org/test/utils/Utils        utils-devs
            org/test/utils/more/        utils-devs
        """.trimIndent()

        val rawCodeOwners = """
            org/test/lib/                       libs-devs
            org/test/lib/LibClass               libs-devs
            org/test/utils/                     libs-devs utils-devs
            org/test/utils/LibUtils             libs-devs
            org/test/utils/Utils                utils-devs
            org/test/utils/more/                utils-devs
            org/test/utils/more/MoreUtils       utils-devs
        """.trimIndent()

        assertCodeOwners("lib1", "main", simplifiedCodeOwners)
        assertCodeOwners("lib1", "main", rawCodeOwners, simplified = false)
        assertCodeOwners("lib1", "test", null)

        lib1TestPasses = true
    }

    @Test
    @Order(4)
    @EnabledIf("getLib1TestPasses")
    fun `lib1 builds successfully`() {
        val build = runBuild(":lib1:build")

        assertEquals(TaskOutcome.UP_TO_DATE, build.task(":lib1:generateCodeOwnersResources")?.outcome)
        assertEquals(null, build.task(":lib1:generateTestCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.NO_SOURCE, build.task(":lib1:generateIntegrationTestCodeOwnersResources")?.outcome)

        lib1BuildPasses = true
    }

    @Test
    @Order(3)
    fun `lib2 tests passes`() {
        val build = runBuild(":lib2:test")

        assertEquals(TaskOutcome.NO_SOURCE, build.task(":lib2:generateDebugCodeOwnersResources")?.outcome)
        assertEquals(null, build.task(":lib2:generateDebugUnitTestCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.NO_SOURCE, build.task(":lib2:generateReleaseCodeOwnersResources")?.outcome)
        assertEquals(null, build.task(":lib2:generateReleaseUnitTestCodeOwnersResources")?.outcome)
        assertCodeOwners("lib2", "main", null)
        assertCodeOwners("lib2", "test", null)

        lib2TestPasses = true
    }

    @Test
    @Order(4)
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
    @Order(5)
    fun `app tests passes`() {
        val build = runBuild(":app:test", ":app:packageDebugAndroidTest")

        assertEquals(TaskOutcome.SUCCESS, build.task(":app:generateDebugCodeOwnersResources")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, build.task(":app:generateDebugUnitTestCodeOwnersResources")?.outcome)
        assertEquals(null, build.task(":app:generateReleaseCodeOwnersResources")?.outcome)
        assertEquals(null, build.task(":app:generateReleaseUnitTestCodeOwnersResources")?.outcome)

        val simplifiedCodeOwners = """
            org/test/app/               app-devs
            org/test/lib/               libs-devs
            org/test/utils/             app-devs libs-devs utils-devs
            org/test/utils/AppUtils     app-devs
            org/test/utils/LibUtils     libs-devs
            org/test/utils/Utils        utils-devs
            org/test/utils/more/        utils-devs
        """.trimIndent()

        val rawCodeOwners = """
           org/test/app/                       app-devs
           org/test/app/AppClass               app-devs
           org/test/app/BuildConfig            app-devs
           org/test/lib/                       libs-devs
           org/test/lib/LibClass               libs-devs
           org/test/utils/                     app-devs libs-devs utils-devs
           org/test/utils/AppUtils             app-devs
           org/test/utils/LibUtils             libs-devs
           org/test/utils/Utils                utils-devs
           org/test/utils/more/                utils-devs
           org/test/utils/more/MoreUtils       utils-devs
        """.trimIndent()

        val simplifiedTestCodeOwners = """
            org/test/app/               app-devs
            org/test/lib/               libs-devs
            org/test/utils/             app-devs libs-devs utils-devs
            org/test/utils/AppUtils     app-devs
            org/test/utils/LibUtils     libs-devs
            org/test/utils/Utils        utils-devs
            org/test/utils/more/        utils-devs
        """.trimIndent()

        val rawTestCodeOwners = """
            org/test/app/                       app-devs
            org/test/app/AppClass               app-devs
            org/test/app/AppOwnersTest          app-devs
            org/test/app/BuildConfig            app-devs
            org/test/lib/                       libs-devs
            org/test/lib/LibClass               libs-devs
            org/test/utils/                     app-devs libs-devs utils-devs
            org/test/utils/AppUtils             app-devs
            org/test/utils/LibUtils             libs-devs
            org/test/utils/Utils                utils-devs
            org/test/utils/more/                utils-devs
            org/test/utils/more/MoreUtils       utils-devs
        """.trimIndent()

        assertCodeOwners("app", "debug", simplifiedCodeOwners)
        assertCodeOwners("app", "debug", rawCodeOwners, simplified = false)
        assertCodeOwners("app", "debugUnitTest", simplifiedTestCodeOwners)
        assertCodeOwners("app", "debugUnitTest", rawTestCodeOwners, simplified = false)
        assertCodeOwners("app", "release", null)
        assertCodeOwners("app", "releaseUnitTest", null)

        androidTestPasses = true
    }

    @Test
    @Order(6)
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
    @Order(7)
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
        simplified: Boolean = true,
    ) {
        val actual = File(buildDir, "$project/build/codeOwners/mappings/$sourceSet${if (simplified) "" else "-raw"}.codeowners")
            .takeIf { it.exists() }
            ?.readText()

        assertEquals(
            expectedContent?.let { "# Generated CODEOWNERS file for module `$project`, source set `$sourceSet`\n\n$it\n" },
            actual
        )
    }

}
