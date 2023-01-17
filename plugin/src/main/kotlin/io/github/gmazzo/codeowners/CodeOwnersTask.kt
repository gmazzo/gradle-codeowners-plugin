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
        val outputDir = outputDirectory.get().apply { asFile.deleteRecursively() }
        val claimed = mutableMapOf<CharSequence, CodeOwnersFile.Entry>()
        val ownership = mutableMapOf<String, Owners>()

        codeOwners.get().filterIsInstance<CodeOwnersFile.Entry>().reversed().forEach { entry ->
            val ignore = FastIgnoreRule(entry.pattern)

            val dirsMatched = mutableSetOf(".")
            sources.asFileTree.visit {
                val rootPath = it.file.toRelativeString(root)

                if (ignore.isMatch(rootPath, it.isDirectory)) {
                    val current = claimed.ownerOf(rootPath)

                    if (current == null || current === entry) {
                        claimed.putIfAbsent(rootPath, entry)

                        if (it.isDirectory) {
                            dirsMatched += it.path

                        } else {
                            val parentPath = it.path.parentPath
                            val isFile = parentPath !in dirsMatched
                            val path = if (isFile) "$parentPath/${it.file.nameWithoutExtension}" else parentPath

                            ownership.addOwner(path, entry.owners, isFile)
                        }
                    }
                }
            }
        }

        val ownersWithDependencies = ownership.toMutableMap()
        runtimeClasspathResources.asFileTree.matching { it.include("**/*.codeowners") }.visit {
            if (!it.isDirectory) {
                val isFile = it.file.nameWithoutExtension.isNotEmpty()
                val path = if (isFile) it.path.removePrefix(".codeowners") else it.path.parentPath

                ownersWithDependencies.addOwner(path, it.file.readLines(), isFile)
            }
        }

        ownership.forEach { (path, owners) ->
            // skip redundant ownership information if parent is the same
            if (ownersWithDependencies.ownerOf(path.parentPath) != owners) {
                val fileName = if (owners.isFile) "$path.codeowners" else "$path/.codeowners"
                val file = outputDir.file(fileName).asFile
                file.parentFile.mkdirs()
                file.writeText(owners.joinToString(separator = "\n", postfix = "\n"))
            }
        }
    }

    private val String.parentPath
        get() = File(this).parent ?: "."

    private tailrec fun <Value> Map<out CharSequence, Value>.ownerOf(path: CharSequence): Value? {
        val current = get(path)
        if (current != null) return current

        val index = path.lastIndexOf(File.separatorChar)
        if (index < 0) return get(".")
        return ownerOf(path.subSequence(0, index))
    }

    private fun MutableMap<String, Owners>.addOwner(
        path: String,
        owners: Collection<String>,
        isFile: Boolean,
    ) = compute(path) { _, current ->
        check(!isFile || current == null) { "Duplicated file ownership entry: $path" }
        Owners(current.orEmpty() + owners, isFile = isFile)
    }

    private data class Owners(
        val owners: Set<String>,
        val isFile: Boolean = false,
    ) : Set<String> by owners

}
