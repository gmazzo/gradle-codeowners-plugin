import io.github.gmazzo.codeowners.CodeOwnersTask
import org.gradle.configurationcache.extensions.capitalized
import kotlin.test.assertEquals

plugins {
    id("io.github.gmazzo.codeowners.jvm")
}

// from here it should not be considered part of the demo project
// this code was added to be able to create test tasks for the generated mapping files

buildscript {
    dependencies {
        classpath(libs.kotlin.test)
    }
}

allprojects {
    val expectedDir = layout.projectDirectory.dir("src/test/expectedMappings")

    afterEvaluate {
        tasks.withType<CodeOwnersTask>().names.forEach { taskName ->
            val expectedRaw = expectedDir.file("${taskName}-raw.txt").asFile
            val expectedSimplified = expectedDir.file("${taskName}-simplified.txt").asFile

            val mappingTask = tasks.named<CodeOwnersTask>(taskName)
            val actualRaw = mappingTask.flatMap { it.rawMappedCodeOwnersFile.asFile }
            val actualSimplified = mappingTask.flatMap { it.mappedCodeOwnersFile.asFile }

            val verifyTask = tasks.register("verify${taskName.capitalized()}Mappings") {
                inputs.files(expectedSimplified, expectedRaw, actualSimplified, actualRaw)
                dependsOn(mappingTask)
                doLast {
                    assertEquals(expectedRaw.readText(), actualRaw.get().readText())
                    assertEquals(expectedSimplified.readText(), actualSimplified.get().readText())
                }
            }

            tasks.named("check") {
                dependsOn(verifyTask)
            }
        }
    }
}
