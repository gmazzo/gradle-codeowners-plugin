package io.github.gmazzo.codeowners

import org.eclipse.jgit.ignore.FastIgnoreRule
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import javax.inject.Inject

@CacheableTask
@Suppress("LeakingThis")
abstract class CodeOwnersTask @Inject constructor(
    sources: SourceDirectorySet
) : DefaultTask() {

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

    @Suppress("CanBePrimaryConstructorProperty") // intentional for better reading
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    val sources: SourceDirectorySet = sources

    @get:InputFiles
    @get:Classpath
    abstract val runtimeClasspathResources: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        outputDirectory.convention(project.layout.dir(project.provider { temporaryDir }))
    }

    @TaskAction
    fun generateCodeOwnersInfo() {
        val root = rootDirectory.asFile.get()

        val entries = codeOwners.get()
            .filterIsInstance<CodeOwnersFile.Entry>()
            .reversed()
            .map { it.owners.toSet() to FastIgnoreRule(it.pattern) }

        val ownership = sortedMapOf<String, Entry>()

        // scans dependency looking for external ownership information and merges it (to increase accuracy)
        runtimeClasspathResources.asFileTree.matching { it.include("**/*.codeowners") }.visit {
            if (it.file.isFile) {
                val isFile = it.file.nameWithoutExtension.isNotEmpty()
                val path = if (isFile) it.path.removePrefix(".codeowners") else it.relativePath.parent.pathString
                val owners = it.file.readLines().toMutableSet()

                ownership[path] = Entry(owners, isFile = isFile, isExternal = true, hasFiles = !isFile)
            }
        }

        // process all files/directories and sets their owners
        fun process(file: File, relativePath: String) {
            val rootPath = file.toRelativeString(root)
            val (owners) = entries.find { (_, ignore) -> ignore.isMatch(rootPath, file.isDirectory) } ?: return
            val targetPath =
                if (file.isDirectory) relativePath
                else relativePath.substringBeforeLast(".")

            ownership.merge(targetPath, Entry(owners, isFile = file.isFile)) { cur, new ->
                Entry(cur.owners + new.owners, isFile = new.isFile)
            }

            if (file.isFile) {
                ownership[File(relativePath).parent ?: ""]?.hasFiles = true
            }
        }
        sources.srcDirs.forEach { process(it, "") }
        sources.asFileTree.visit { process(it.file, it.path) }

        fun shouldWrite(path: String, entry: Entry): Boolean {
            if (entry.isExternal) return false
            if (entry.isFile || entry.hasFiles) {
                val parent = ownership[File(path).parent ?: ""] ?: return true
                return parent === entry || !parent.hasFiles || entry.owners != parent.owners
            }
            return false
        }

        val outputDir = outputDirectory.get().apply { asFile.deleteRecursively() }
        ownership.entries.forEach { (path, entry) ->
            if (shouldWrite(path, entry)) {
                val fileName = if (entry.isFile) "$path.codeowners" else "${path.ifEmpty { "." }}/.codeowners"

                with(outputDir.file(fileName).asFile) {
                    parentFile.mkdirs()
                    writeText(entry.owners.sorted().joinToString(separator = "\n", postfix = "\n"))
                }
            }
        }
    }

    private data class Entry(
        val owners: Set<String>,
        val isFile: Boolean,
        val isExternal: Boolean = false,
        var hasFiles: Boolean = false,
    )

}
