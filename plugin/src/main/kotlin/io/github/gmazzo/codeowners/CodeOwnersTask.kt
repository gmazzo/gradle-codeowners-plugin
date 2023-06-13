package io.github.gmazzo.codeowners

import org.eclipse.jgit.ignore.FastIgnoreRule
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import java.io.File
import java.util.*
import kotlin.math.ceil
import kotlin.math.max

@CacheableTask
@Suppress("LeakingThis")
abstract class CodeOwnersTask : DefaultTask() {

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

    @get:Input
    abstract val codeOwners: Property<CodeOwnersFile>

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

    @get:Input
    @get:Optional
    abstract val mappedCodeOwnersFileHeader: Property<String>

    @get:Optional
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Optional
    @get:OutputFile
    abstract val mappedCodeOwnersFile: RegularFileProperty

    init {
        outputDirectory.convention(project.layout.dir(project.provider { temporaryDir }))
    }

    @TaskAction
    fun generateCodeOwnersInfo() {
        val ownership = sortedMapOf<String, Entry>()

        ownership.collectFromDependencies()
        ownership.collectFromSources()

        ownership.writeJavaResourcesOwnershipInfo()
        ownership.writeMappedCodeOwnersFile()
    }

    /**
     * Scans dependency looking for external ownership information and merges it (to increase accuracy)
     */
    private fun MutableMap<String, Entry>.collectFromDependencies() {
        transitiveCodeOwners.files.asSequence()
            .flatMap { CodeOwnersFile(it.readText()) }
            .filterIsInstance<CodeOwnersFile.Entry>()
            .forEach {
                val isFile = it.pattern.endsWith('/')

                compute(path) { _, acc ->
                    Entry(
                        owners = TreeSet(acc?.owners.orEmpty() + it.owners),
                        isFile = isFile && acc?.isFile != false,
                        isExternal = true,
                        hasFiles = !isFile || acc?.hasFiles == true,
                    )
                }
            }
    }

    /**
     * Process all files/directories and sets their owners
     */
    private fun MutableMap<String, Entry>.collectFromSources() {
        logger.info("Processing sources...")

        val root = rootDirectory.asFile.get()
        val entries = codeOwners.get()
            .filterIsInstance<CodeOwnersFile.Entry>()
            .reversed()
            .map { it.owners.toSet() to FastIgnoreRule(it.pattern) }

        sourcesFiles.visit {
            val rootPath = file.toRelativeString(root)
            val (owners) = entries.find { (_, ignore) -> ignore.isMatch(rootPath, isDirectory) } ?: return@visit
            val targetPath =
                if (isDirectory) path
                else path.substringBeforeLast(".")

            compute(targetPath) { _, acc ->
                Entry(
                    owners = TreeSet(acc?.owners.orEmpty() + owners),
                    isFile = !isDirectory && acc?.isFile != false,
                    isExternal = false,
                    hasFiles = acc?.hasFiles ?: false,
                )
            }

            if (!isDirectory) {
                this@collectFromSources[relativePath.parent.pathString]?.hasFiles = true
            }
        }
    }

    /**
     * Generates `resources` entries meant to be used by [io.github.gmazzo.codeowners.codeOwnersOf] function
     */
    private fun MutableMap<String, Entry>.writeJavaResourcesOwnershipInfo() {
        val outputDir = outputDirectory.orNull?.apply { asFile.deleteRecursively() } ?: return

        val redundancy = RedundancyHelper(this, includeExternals = false)

        logger.info("Generating output from $size ownership information entries...")
        entries.forEach { (path, entry) ->
            if (redundancy.shouldWrite(path, entry)) {
                redundancy.written.add(entry)

                val fileName = if (entry.isFile) "$path.codeowners" else "${path.ifEmpty { "." }}/.codeowners"

                with(outputDir.file(fileName).asFile) {
                    parentFile.mkdirs()
                    writeText(entry.owners.joinToString(separator = "\n", postfix = "\n"))
                }
            }
        }

        logger.info("Generated ${redundancy.written.size} simplified ownership information entries.")
    }

    /**
     * Generates a new `.codeowners` file, where the ownership information is mapped to the relative path of the source folders
     */
    private fun MutableMap<String, Entry>.writeMappedCodeOwnersFile() {
        val mappedFile = mappedCodeOwnersFile.asFile.orNull?.apply { parentFile.mkdirs() } ?: return

        val entries = mutableListOf<CodeOwnersFile.Part>()

        mappedCodeOwnersFileHeader.orNull?.let { entries.add(CodeOwnersFile.Comment(comment = it)) }

        val redundancy = RedundancyHelper(this, includeExternals = true)
        this@writeMappedCodeOwnersFile.forEach { (path, entry) ->
            if (redundancy.shouldWrite(path, entry)) {
                redundancy.written.add(entry)

                entries.add(CodeOwnersFile.Entry(
                    pattern = if (!entry.isFile && !path.endsWith('/')) "$path/" else path,
                    owners = entry.owners.toList()
                ))
            }
        }

        mappedFile.writeText(CodeOwnersFile(entries).content)
    }

    private data class Entry(
        val owners: SortedSet<String>,
        val isFile: Boolean,
        val isExternal: Boolean = false,
        var hasFiles: Boolean = false,
    )

    private class RedundancyHelper(
        private val ownership: Map<String, Entry>,
        private val includeExternals: Boolean,
    ) {

        val written = mutableSetOf<Entry>()

        fun shouldWrite(path: String, entry: Entry): Boolean {
            if (!includeExternals && entry.isExternal) return false
            if (entry.isFile || entry.hasFiles) {
                if (path == "") return true

                var parent: File? = File(path).parentFile
                do {
                    val parentEntry = ownership[parent?.path ?: ""]
                    if (parentEntry != null) {
                        if (parentEntry.owners != entry.owners) return true
                        if (parentEntry in written) return false
                    }

                    parent = parent?.parentFile
                } while (parent != null)
                return true
            }
            return false
        }

    }

}
