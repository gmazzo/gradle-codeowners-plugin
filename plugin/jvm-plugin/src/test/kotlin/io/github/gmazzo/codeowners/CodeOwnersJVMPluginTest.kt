package io.github.gmazzo.codeowners

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodeOwnersJVMPluginTest {

    private val root = ProjectBuilder.builder()
        .withName("root")
        .withGradleUserHomeDir(File(".gradle"))
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

    private val child3 = ProjectBuilder.builder()
        .withName("child3")
        .withParent(root)
        .build()

    private val codeOwnersFile = root.file("CODEOWNERS")

    @BeforeAll
    fun setup() {
        root.rootDir.deleteRecursively()

        root.allprojects {
            apply(plugin = "java")
            apply<CodeOwnersJVMPlugin>()

            repositories.mavenCentral()
            configurations.configureEach {
                val (group, module) = BuildConfig.CORE_DEPENDENCY.split(':')
                exclude(group = group, module = module)
            }
        }
        root.apply(plugin = "org.jetbrains.kotlin.jvm")
        child2.apply(plugin = "groovy")
        child3.apply(plugin = "org.jetbrains.kotlin.jvm")

        sequenceOf(
            "src/main/java/com/test/app/App.java",
            "src/main/java/com/test/app/admin/AdminFactory.java",
            "src/main/java/com/test/app/child1/Child1Factory.java",
            "src/main/kotlin/com/test/app/AppData.kt",
            "src/main/kotlin/com/test/app/child2/Child2Factory.kt",

            "admin/src/main/java/com/test/admin/Admin.java",

            "child1/src/main/java/com/test/child1/Piece1.java",
            "child1/src/main/java/com/test/child1/data/dtos/Piece1DTO.java",

            "child2/src/main/java/com/test/child2/Piece2.java",
            "child2/src/main/groovy/Main.groovy",
            "child2/src/main/groovy/env-dev/Helper.groovy",

            "child3/src/main/java/com/test/child3/Piece3.java",
            "child3/src/main/java/com/test/child3/Piece3Data.java",
            "child3/src/main/java/com/test/child3/a/Piece3A.java",
            "child3/src/main/kotlin/com/test/child3/Piece3.kt",
            "child3/src/main/kotlin/com/test/child3/Piece3Stubs.kt",
            "child3/src/main/kotlin/com/test/child3/b/Piece3B.kt",

        ).map(root::file).onEach { it.parentFile.mkdirs() }.forEach(File::createNewFile)

        codeOwnersFile.writeText(
            """
            # this is a test CODEOWNERS file
            *                   app-devs
            *.kt                app-devs kotlin-devs
            child1/             child1-devs
            child2/             child2-devs app-devs 
            child3/**/java      child3-java 
            child3/**/kotlin    child3-kotlin
            /admin              app-devs admin-devs
            **/groovy/env-*     scripting-devs
            """.trimIndent())
    }

    @Test
    fun `generates root code package info correctly`() = root.testGenerateCodeOwners(
        "com/test/app/.codeowners" to setOf("app-devs"),
        "com/test/app/AppData.codeowners" to setOf("app-devs", "kotlin-devs"),
        "com/test/app/child1/.codeowners" to setOf("child1-devs"),
        "com/test/app/child2/.codeowners" to setOf("child2-devs", "app-devs"),
        expectedMappings = """
            # Generated CODEOWNERS file for module `root`, source set `main`
            
            com/test/app/               app-devs
            com/test/app/AppData        app-devs kotlin-devs
            com/test/app/child1/        child1-devs
            com/test/app/child2/        app-devs child2-devs
            
        """.trimIndent()
    )

    @Test
    fun `generates admin code package info correctly`() = admin.testGenerateCodeOwners(
        "com/test/admin/.codeowners" to setOf("app-devs", "admin-devs"),
        expectedMappings = """
            # Generated CODEOWNERS file for module `admin`, source set `main`
            
            com/test/admin/     admin-devs app-devs
            
        """.trimIndent()
    )

    @Test
    fun `generates child1 code package info correctly`() = child1.testGenerateCodeOwners(
        "com/test/child1/.codeowners" to setOf("child1-devs"),
        expectedMappings = """
            # Generated CODEOWNERS file for module `child1`, source set `main`
            
            com/test/child1/        child1-devs
            
        """.trimIndent()
    )

    @Test
    fun `generates child2 code package info correctly`() = child2.testGenerateCodeOwners(
        "Main.codeowners" to setOf("child2-devs", "app-devs"),
        "com/test/child2/.codeowners" to setOf("child2-devs", "app-devs"),
        "env-dev/.codeowners" to setOf("scripting-devs"),
        expectedMappings = """
            # Generated CODEOWNERS file for module `child2`, source set `main`
            
            Main                    app-devs child2-devs
            com/test/child2/        app-devs child2-devs
            env-dev/                scripting-devs
            
        """.trimIndent()
    )

    @Test
    fun `generates child3 code package info correctly`() = child3.testGenerateCodeOwners(
        "com/test/child3/.codeowners" to setOf("child3-kotlin", "child3-java"),
        "com/test/child3/Piece3Data.codeowners" to setOf("child3-java"),
        "com/test/child3/Piece3Stubs.codeowners" to setOf("child3-kotlin"),
        "com/test/child3/a/.codeowners" to setOf("child3-java"),
        "com/test/child3/b/.codeowners" to setOf("child3-kotlin"),
        expectedMappings = """
            # Generated CODEOWNERS file for module `child3`, source set `main`
            
            com/test/child3/                child3-java child3-kotlin
            com/test/child3/Piece3Data      child3-java
            com/test/child3/Piece3Stubs     child3-kotlin
            com/test/child3/a/              child3-java
            com/test/child3/b/              child3-kotlin
            
        """.trimIndent()
    )

    private fun Project.testGenerateCodeOwners(vararg expectedInfos: Pair<String, Set<String>>, expectedMappings: String) {
        tasks.withType<CodeOwnersTask>().all { generateCodeOwnersInfo() }

        val actualInfos = layout.buildDirectory.dir("codeOwners/resources/main").get().let { dir ->
            dir.asFileTree.files
                .sorted()
                .map { it.toRelativeString(dir.asFile) to it.readLines().toSet() }
        }

        fun List<Pair<String, Set<String>>>.asText() = joinToString(separator = "\n") { (path, owners) ->
            "$path -> ${owners.sorted().joinToString(separator = " ")}"
        }

        assertEquals(expectedInfos.toList().asText(), actualInfos.asText())

        val actualMappings = layout.buildDirectory.file("codeOwners/mappings/main.codeowners").get().asFile.readText()
        assertEquals(expectedMappings, actualMappings)
    }

}
