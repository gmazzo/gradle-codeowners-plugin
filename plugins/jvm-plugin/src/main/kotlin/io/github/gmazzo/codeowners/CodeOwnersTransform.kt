package io.github.gmazzo.codeowners

import io.github.gmazzo.codeowners.matcher.CodeOwnersFile
import java.util.zip.ZipInputStream
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

@CacheableTransform
internal abstract class CodeOwnersTransform : TransformAction<TransformParameters.None> {

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputArtifact
    abstract val inputJar: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val jar = inputJar.get().asFile

        outputs.file(jar.nameWithoutExtension + ".codeowners").writer().use { out ->
            out.append("# Generated .codeowners for ")
            out.appendLine(jar.name)
            out.appendLine()

            ZipInputStream(jar.inputStream()).use { zip ->
                val reader = zip.bufferedReader()
                val entries = generateSequence(zip.nextEntry) { zip.nextEntry }
                    .filter { !it.isDirectory && it.name.endsWith(".codeowners") }
                    .map {
                        CodeOwnersFile.Entry(
                            pattern = it.name.removeSuffix(".codeowners"),
                            owners = reader.lineSequence().toList(),
                        )
                    }
                    .toList()
                    .sortedBy { it.pattern }

                out.append(CodeOwnersFile(entries).content)
            }
        }
    }

}
