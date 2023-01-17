package io.github.gmazzo.codeowners

import io.mockk.*
import org.gradle.api.artifacts.transform.TransformOutputs
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test
import java.io.File

class CodeOwnersTransformTest {

    private val workingDir = File(".").apply {
        deleteRecursively()
        mkdirs()
    }

    private val lib1Jar = File(this::class.java.getResource("/lib1.jar")!!.toURI().path)

    private val transform: CodeOwnersTransform = spyk {
        every { inputJar } returns mockk {
            every { get() } returns mockk {
                every { asFile } returns lib1Jar
            }
        }
    }

    private val outputs: TransformOutputs = mockk {
        every { file(any()) } answers { File(workingDir, firstArg<String>()).apply { parentFile.mkdirs() } }
    }

    @Test
    fun `transform, should process the input jar and generate the right output`() {
        transform.transform(outputs)

        verify {
            outputs.file("org/test/lib/.codeowners")
            outputs.file("org/test/utils/.codeowners")
        }
        confirmVerified(outputs)

        val outputFiles = workingDir.walkTopDown()
            .filter { it.isFile }
            .map { it.toRelativeString(workingDir) to it.readText().trim() }
            .toList()

        assertIterableEquals(
            listOf(
                "org/test/utils/.codeowners" to "kotlin-devs",
                "org/test/lib/.codeowners" to "kotlin-devs",
            ),
            outputFiles
        )
    }

}
