package io.github.gmazzo.codeowners

import org.gradle.api.artifacts.transform.*
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.io.path.pathString
import kotlin.streams.asSequence

@CacheableTransform
internal abstract class CodeOwnersTransform : TransformAction<TransformParameters.None> {

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputArtifact
    abstract val inputJar: Provider<FileSystemLocation>

    private val inputUri get() = URI.create("jar:file:${inputJar.get().asFile.path}")

    override fun transform(outputs: TransformOutputs) = FileSystems.newFileSystem(inputUri, emptyMap<String, Any>()).use { zip ->
        zip.rootDirectories
            .flatMap { Files.walk(it).asSequence() }
            .filter { !Files.isDirectory(it) && it.fileName.endsWith(".codeowners") }
            .forEach { Files.copy(it, outputs.file(it.root.relativize(it).pathString).toPath()) }
    }

}
