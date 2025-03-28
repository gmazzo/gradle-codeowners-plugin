package io.github.gmazzo.codeowners

import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.internal.provider.Providers
import org.gradle.api.provider.Provider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class CodeOwnersTransformTest {

    private val lib1Jar = File(this::class.java.getResource("/lib1.jar")!!.toURI().path)

    private val transform = object : CodeOwnersTransform() {
        override fun getParameters() = throw UnsupportedOperationException()
        override val inputJar: Provider<FileSystemLocation> = Providers.of(FileSystemLocation { lib1Jar })
    }

    private val outputsDir by lazy { Files.createTempDirectory("transformsTestOutputs").toFile() }

    private val outputs: TransformOutputs = object : TransformOutputs {
        override fun dir(path: Any) = throw UnsupportedOperationException()
        override fun file(path: Any) = File(outputsDir, path as String).apply { parentFile.mkdirs() }
    }

    @Test
    fun `transform, should process the input jar and generate the right output`() {
        transform.transform(outputs)

        val outputContent = File(outputsDir, "${lib1Jar.nameWithoutExtension}.codeowners").readText()

        assertEquals(
            """
                # Generated .codeowners for lib1.jar

                org/test/lib/       kotlin-devs java-devs
                org/test/utils/     kotlin-devs

            """.trimIndent(),
            outputContent
        )
    }

    @AfterEach
    fun cleanUp() {
        outputsDir.deleteRecursively()
    }

}
