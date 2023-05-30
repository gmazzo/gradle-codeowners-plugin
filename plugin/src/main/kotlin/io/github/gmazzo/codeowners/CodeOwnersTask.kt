package io.github.gmazzo.codeowners

import org.eclipse.jgit.ignore.FastIgnoreRule
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
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

    @get:Internal
    abstract val runtimeClasspath: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    internal val runtimeClasspathCodeOwners: FileTree =
        runtimeClasspath.asFileTree.matching { it.include("**/*.codeowners") }

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:OutputFile
    @get:Optional
    abstract val mappedCodeOwnersFile: RegularFileProperty

    init {
        outputDirectory.convention(project.layout.dir(project.provider { temporaryDir }))
    }

    @TaskAction
    fun generateCodeOwnersInfo() {
        val root = rootDirectory.asFile.get()

        logger.trace("Computing CODEOWNERS file...")
        val entries = codeOwners.get()
            .filterIsInstance<CodeOwnersFile.Entry>()
            .reversed()
            .map { it.owners.toSet() to FastIgnoreRule(it.pattern) }

        val ownership = sortedMapOf<String, Entry>()

        // scans dependency looking for external ownership information and merges it (to increase accuracy)
        logger.info("Scanning dependencies...")
        runtimeClasspathCodeOwners.visit {
            if (it.file.isFile) {
                val isFile = it.file.nameWithoutExtension.isNotEmpty()
                val path = if (isFile) it.path.removePrefix(".codeowners") else it.relativePath.parent.pathString
                val owners = it.file.readLines().toMutableSet()

                ownership[path] = Entry(owners, isFile = isFile, isExternal = true, hasFiles = !isFile)
            }
        }

        // process all files/directories and sets their owners
        logger.info("Processing sources...")
        sourcesFiles.visit {
            val rootPath = it.file.toRelativeString(root)
            val (owners) = entries.find { (_, ignore) -> ignore.isMatch(rootPath, it.isDirectory) } ?: return@visit
            val targetPath =
                if (it.isDirectory) it.path
                else it.path.substringBeforeLast(".")

            ownership.merge(targetPath, Entry(owners, isFile = !it.isDirectory)) { cur, new ->
                Entry(cur.owners + new.owners, isFile = new.isFile)
            }

            if (!it.isDirectory) {
                ownership[it.relativePath.parent.pathString]?.hasFiles = true
            }
        }

        val written = mutableSetOf<Entry>()
        fun shouldWrite(path: String, entry: Entry): Boolean {
            if (entry.isExternal) return false
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

        logger.info("Generating output from ${ownership.size} ownership information entries...")
        val mappings = mutableListOf<Pair<String, String>>()
        var indent = 0
        val outputDir = outputDirectory.get().apply { asFile.deleteRecursively() }
        ownership.entries.forEach { (path, entry) ->
            if (shouldWrite(path, entry)) {
                written.add(entry)

                val owners = entry.owners.sorted()

                mappings.add(path to owners.joinToString(separator = " "))
                indent = max(indent, ceil(path.length / 4f).toInt() + 2)

                val fileName = if (entry.isFile) "$path.codeowners" else "${path.ifEmpty { "." }}/.codeowners"

                with(outputDir.file(fileName).asFile) {
                    parentFile.mkdirs()
                    writeText(owners.joinToString(separator = "\n", postfix = "\n"))
                }
            }
        }
        mappedCodeOwnersFile.asFile.orNull?.apply { parentFile.mkdirs() }?.writeMappings(mappings, indent * 4)

        logger.info("Generated ${written.size} simplified ownership information entries.")
    }

    private fun File.writeMappings(mappings: List<Pair<String, String>>, indent: Int) = printWriter().use {
        mappings.forEach { (path, owners) ->
            it.print(path)
            (path.length until indent).forEach { _ -> it.print(" ") }
            it.println(owners)
        }
    }

    private data class Entry(
        val owners: Set<String>,
        val isFile: Boolean,
        val isExternal: Boolean = false,
        var hasFiles: Boolean = false,
    )

}
