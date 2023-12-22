package io.github.gmazzo.codeowners

import io.github.gmazzo.codeowners.matcher.CodeOwnersFile
import io.github.gmazzo.codeowners.matcher.CodeOwnersMatcher
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

@CacheableTask
@Suppress("LeakingThis")
abstract class CodeOwnersReportTask : DefaultTask() {

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    /**
     * Helper input to declare that we only care about paths and not file contents on [rootDirectory] and [sources]
     *
     * [Incorrect use of the `@Input` annotation](https://docs.gradle.org/7.6/userguide/validation_problems.html#incorrect_use_of_input_annotation)
     */
    @get:Input
    internal val rootDirectoryPath =
        rootDirectory.map { it.asFile.toRelativeString(project.rootDir) }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val codeOwnersFile: RegularFileProperty

    @get:Internal
    abstract val sources: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:SkipWhenEmpty
    internal val sourcesFiles: FileTree = sources.asFileTree

    @get:Input
    @get:Optional
    abstract val codeOwnersReportHeader: Property<String>

    @get:OutputFile
    abstract val codeOwnersReportFile: RegularFileProperty

    @TaskAction
    fun generateCodeOwnersInfo() {
        val root = rootDirectory.asFile.get()
        val matcher = CodeOwnersMatcher(
            root,
            codeOwnersFile.asFile.get().useLines { CodeOwnersFile(it) }
        )
        val entries = mutableMapOf<String, MutableSet<String>>()

        sourcesFiles.visit {
            if (!isDirectory) {
                val owners = matcher.ownerOf(file, isDirectory)

                if (owners != null) {
                    entries.computeIfAbsent(path) { mutableSetOf() }.addAll(owners)
                }
            }
        }

        val header = listOfNotNull(codeOwnersReportHeader.orNull?.let(CodeOwnersFile::Comment))
        val file = CodeOwnersFile(header + entries.map { (key, value) ->
            CodeOwnersFile.Entry(pattern = key, owners = value.toList()) })

        codeOwnersReportFile.asFile.get().writeText(file.content)
    }

}
