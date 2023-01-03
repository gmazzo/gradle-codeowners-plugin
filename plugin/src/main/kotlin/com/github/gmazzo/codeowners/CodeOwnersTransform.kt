package com.github.gmazzo.codeowners

import org.gradle.api.artifacts.transform.*
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.util.zip.ZipFile
import kotlin.streams.asSequence

@CacheableTransform
abstract class CodeOwnersTransform : TransformAction<TransformParameters.None> {

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputArtifact
    abstract val inputJar: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) = ZipFile(inputJar.get().asFile).use { zip ->
        zip.stream().asSequence()
            .filter { !it.isDirectory && it.name.endsWith(".codeowners") }
            .forEach { outputs.file(it.name).outputStream().use(zip.getInputStream(it)::copyTo)  }
    }

}
