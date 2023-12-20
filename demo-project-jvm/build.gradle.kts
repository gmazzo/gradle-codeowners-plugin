import io.github.gmazzo.codeowners.CodeOwnersTask
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

    val filesToVerify = objects.mapProperty<RegularFile, RegularFile>()
    tasks.withType<CodeOwnersTask>().configureEach {
        filesToVerify.put(expectedDir.file("${name}-raw.codeowners"), rawMappedCodeOwnersFile)
        filesToVerify.put(expectedDir.file("${name}-simplified.codeowners"), mappedCodeOwnersFile)
    }

    val verifyTask = tasks.register("verifyCodeOwnersMappings") {
        inputs.files(filesToVerify.keySet(), filesToVerify.map { it.values })
        doLast {
            fun File.readTextIfExist() = if (exists()) readText() else null

            filesToVerify.get().forEach { (expected, actual) ->
                assertEquals(expected.asFile.readTextIfExist(), actual.asFile.readTextIfExist())
            }
        }
    }

    plugins.withId("base") {
        tasks.named("check") {
            dependsOn(verifyTask)
        }
    }
}
