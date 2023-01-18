package io.github.gmazzo.codeowners

import org.eclipse.jgit.ignore.FastIgnoreRule
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
@Suppress("LeakingThis")
abstract class CodeOwnersTask : DefaultTask() {

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Input
    abstract val codeOwners: Property<CodeOwnersFile>

    @get:Internal
    abstract val sources: ConfigurableFileCollection

    @get:Internal
    abstract val runtimeClasspath: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    internal val runtimeClasspathCodeOwners: FileTree =
        runtimeClasspath.asFileTree.matching { it.include("**/*.codeowners") }

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /**
     * Helper input to declare that we only care about paths and not file contents on [rootDirectory] and [sources]
     *
     * [Incorrect use of the `@Input` annotation](https://docs.gradle.org/7.6/userguide/validation_problems.html#incorrect_use_of_input_annotation)
     */
    @get:Input
    internal abstract val sourcesPaths: SetProperty<String>

    init {
        outputDirectory.convention(project.layout.dir(project.provider { temporaryDir }))

        sourcesPaths.add(rootDirectory.map { it.asFile.toRelativeString(project.rootDir) })
        sourcesPaths.addAll(sources.asFileTree.asSequence().map { it.toRelativeString(project.rootDir) }.asIterable())
        sourcesPaths.disallowChanges()

        onlyIf { !sources.isEmpty }
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
        sources.asFileTree.visit {
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

        fun shouldWrite(path: String, entry: Entry): Boolean {
            if (entry.isExternal) return false
            if (entry.isFile || entry.hasFiles) {
                val parent = ownership[File(path).parent ?: ""] ?: return true
                return parent === entry || !parent.hasFiles || entry.owners != parent.owners
            }
            return false
        }

        logger.info("Generating output from ${ownership.size} ownership information entries...")
        val outputDir = outputDirectory.get().apply { asFile.deleteRecursively() }
        var outputCount = 0
        ownership.entries.forEach { (path, entry) ->
            if (shouldWrite(path, entry)) {
                val fileName = if (entry.isFile) "$path.codeowners" else "${path.ifEmpty { "." }}/.codeowners"

                with(outputDir.file(fileName).asFile) {
                    parentFile.mkdirs()
                    writeText(entry.owners.sorted().joinToString(separator = "\n", postfix = "\n"))
                }
                outputCount++
            }
        }

        logger.info("Generated $outputCount simplified ownership information entries.")
    }

    private data class Entry(
        val owners: Set<String>,
        val isFile: Boolean,
        val isExternal: Boolean = false,
        var hasFiles: Boolean = false,
    )

}
