package io.github.gmazzo.codeowners

import io.github.gmazzo.codeowners.matcher.CodeOwnersFile
import io.github.gmazzo.codeowners.matcher.CodeOwnersMatcher
import java.io.File
import java.util.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

@CacheableTask
@Suppress("LeakingThis")
abstract class CodeOwnersResourcesTask : DefaultTask() {

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    /**
     * Helper input to declare that we only care about paths and not file contents on [rootDirectory] and [sources]
     *
     * [Incorrect use of the `@Input` annotation](https://docs.gradle.org/7.6/userguide/validation_problems.html#incorrect_use_of_input_annotation)
     */
    @get:Input
    internal val rootDirectoryPath = project.rootDir.let { rootDir ->
        rootDirectory.map { it.asFile.toRelativeString(rootDir) }
    }

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

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val transitiveCodeOwners: ConfigurableFileCollection

    @get:Optional
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Optional
    @get:OutputFile
    abstract val rawMappedCodeOwnersFile: RegularFileProperty

    @get:Optional
    @get:OutputFile
    abstract val simplifiedMappedCodeOwnersFile: RegularFileProperty

    init {
        outputDirectory.convention(project.layout.dir(project.provider { temporaryDir }))
    }

    @TaskAction
    fun generateCodeOwnersInfo() {
        val ownership = sortedMapOf<String, Entry>()

        collectFromDependencies(ownership)
        collectFromSources(ownership)

        writeCodeOwnersInfo(ownership)
    }

    /**
     * Scans dependency looking for external ownership information and merges it (to increase accuracy)
     */
    private fun collectFromDependencies(ownership: MutableMap<String, Entry>) {
        transitiveCodeOwners.asFileTree.files.asSequence()
            .flatMap { CodeOwnersFile(it.readText()) }
            .filterIsInstance<CodeOwnersFile.Entry>()
            .forEach {
                ownership.compute(it.pattern) { _, acc ->
                    Entry(
                        owners = TreeSet(acc?.owners.orEmpty() + it.owners),
                        isExternal = true,
                        hasOwnFiles = true,
                    )
                }
            }
    }

    /**
     * Process all files/directories and sets their owners
     */
    private fun collectFromSources(ownership: MutableMap<String, Entry>) {
        logger.info("Processing sources...")

        val root = rootDirectory.asFile.get()
        val codeOwners = codeOwnersFile.asFile.get().useLines { CodeOwnersFile(it) }
        val matcher = CodeOwnersMatcher(root, codeOwners)

        sourcesFiles.visit {
            val owners = matcher.ownerOf(file, isDirectory) ?: return@visit
            val targetPath =
                if (isDirectory) path.appendSuffix("/")
                else path.substringBeforeLast(".")

            ownership.compute(targetPath) { _, acc ->
                Entry(
                    owners = TreeSet(acc?.owners.orEmpty() + owners),
                    isExternal = false,
                    hasOwnFiles = acc?.hasOwnFiles ?: !isDirectory,
                )
            }

            if (!isDirectory) {
                ownership[relativePath.parent.pathString.appendSuffix("/")]?.hasOwnFiles = true
            }
        }
    }

    private fun writeCodeOwnersInfo(ownership: MutableMap<String, Entry>) {
        val resourcesDir = outputDirectory.orNull?.apply { asFile.deleteRecursively() }
        val rawFile = rawMappedCodeOwnersFile.asFile.orNull?.apply { parentFile.mkdirs() }
        val simplifiedFile = simplifiedMappedCodeOwnersFile.asFile.orNull?.apply { parentFile.mkdirs() }

        val rawEntries = LinkedList<CodeOwnersFile.Part>()
        val simplifiedEntries = LinkedList<CodeOwnersFile.Part>()

        val rawHelper = RedundancyHelper(ownership, simplified = false)
        val simplifiedHelper = RedundancyHelper(ownership, simplified = true)

        fun RedundancyHelper.tryAdd(path: String, entry: Entry, into: MutableList<in CodeOwnersFile.Entry>): Boolean {
            if (shouldWrite(path, entry)) {
                written.add(entry)

                into.add(
                    CodeOwnersFile.Entry(
                        pattern = path,
                        owners = entry.owners.toList()
                    )
                )
                return true
            }
            return false
        }

        ownership.forEach { (path, entry) ->
            rawHelper.tryAdd(path, entry, rawEntries)

            if (simplifiedHelper.tryAdd(path, entry, simplifiedEntries)) {
                resourcesDir?.file("$path.codeowners")?.asFile?.apply {
                    parentFile.mkdirs()
                    writeText(entry.owners.joinToString(separator = "\n", postfix = "\n"))
                }
            }
        }

        rawFile?.writeText(CodeOwnersFile(rawEntries).content)
        simplifiedFile?.writeText(CodeOwnersFile(simplifiedEntries).content)
    }

    private data class Entry(
        val owners: SortedSet<String>,
        val isExternal: Boolean = false,
        var hasOwnFiles: Boolean = false,
    )

    private class RedundancyHelper(
        private val ownership: Map<String, Entry>,
        private val simplified: Boolean,
    ) {

        val written = mutableSetOf<Entry>()

        fun shouldWrite(path: String, entry: Entry): Boolean {
            if (entry.hasOwnFiles) {
                if (path == "") return true

                if (simplified) {
                    var parent: File? = File(path).parentFile
                    do {
                        val parentEntry = ownership[parent?.path?.appendSuffix("/") ?: ""]
                        if (parentEntry != null) {
                            if (parentEntry.owners != entry.owners) return true
                            if (parentEntry in written) return false
                        }

                        parent = parent?.parentFile
                    } while (parent != null)
                    return !entry.isExternal
                }
                return true
            }
            return false
        }

    }

    private companion object {

        private fun String.appendSuffix(suffix: String) =
            if (endsWith(suffix)) this else "$this$suffix"

    }

}
