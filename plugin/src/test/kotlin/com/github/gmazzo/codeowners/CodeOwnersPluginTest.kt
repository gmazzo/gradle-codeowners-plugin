package com.github.gmazzo.codeowners

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodeOwnersPluginTest {

    private val root = ProjectBuilder.builder()
        .withProjectDir(File("."))
        .build()

    private val admin = ProjectBuilder.builder()
        .withName("admin")
        .withParent(root)
        .build()

    private val child1 = ProjectBuilder.builder()
        .withName("child1")
        .withParent(root)
        .build()

    private val child2 = ProjectBuilder.builder()
        .withName("child2")
        .withParent(root)
        .build()

    private val codeOwnersFile = root.file("CODEOWNERS")

    @BeforeAll
    fun setup() {
        root.rootDir.deleteRecursively()

        root.allprojects {
            it.apply(plugin = "java")
            it.apply<CodeOwnersPlugin>()
        }
        root.apply(plugin = "org.jetbrains.kotlin.jvm")
        child2.apply(plugin = "groovy")

        sequenceOf(
            "src/main/java/com/test/app/App.java",
            "src/main/java/com/test/app/admin/AdminFactory.kt",
            "src/main/java/com/test/app/child1/Child1Factory.kt",
            "src/main/kotlin/com/test/app/child2/Child2Factory.kt",

            "admin/src/main/java/com/test/admin/Admin.java",

            "child1/src/main/java/com/test/child1/Piece1.java",

            "child2/src/main/java/com/test/child2/Piece2.java",
            "child2/src/main/groovy/env-dev/Helper.groovy",

        ).map(root::file).onEach { it.parentFile.mkdirs() }.forEach(File::createNewFile)

        codeOwnersFile.writeText(
            """
            # this is a test CODEOWNERS file
            *                   app-devs
            child1/             child1-devs
            child2/             child2-devs app-devs 
            /admin              app-devs admin-devs
            **/groovy/env-*     scripting-devs
            """.trimIndent())
    }

    @Test
    fun `generates root code package info correctly`() = root.testGeneratePackageInfo(
        "com" to listOf("app-devs"),
        "com.test.app.child1" to listOf("child1-devs"),
        "com.test.app.child2" to listOf("child2-devs", "app-devs"),
    )

    @Test
    fun `generates admin code package info correctly`() = admin.testGeneratePackageInfo(
        "com" to listOf("app-devs", "admin-devs"),
    )

    @Test
    fun `generates child1 code package info correctly`() = child1.testGeneratePackageInfo(
        "com" to listOf("child1-devs"),
    )

    @Test
    fun `generates child2 code package info correctly`() = child2.testGeneratePackageInfo(
        "com" to listOf("child2-devs", "app-devs"),
        "env-dev" to listOf("scripting-devs"),
    )

    private fun Project.testGeneratePackageInfo(vararg expected: Pair<String, List<String>>) {
        tasks.withType<CodeOwnersTask>().all { it.generatePackagesInfo() }

        val expectedInfos = expected.map { (packageName, owners) ->
            "${packageName.replace('.', '/')}/package-info.java" to """
                @com.github.gmazzo.codeowners.CodeOwner(${owners.joinToString(separator = ", ") { "\"$it\"" }})
                package $packageName;
                """.trimIndent()
        }

        val actualInfos = layout.buildDirectory.dir("codeOwners/main").get().let { dir ->
            dir.asFileTree.files
                .sorted()
                .map { it.toRelativeString(dir.asFile) to it.readText() }
        }

        assertIterableEquals(expectedInfos, actualInfos)
    }

}