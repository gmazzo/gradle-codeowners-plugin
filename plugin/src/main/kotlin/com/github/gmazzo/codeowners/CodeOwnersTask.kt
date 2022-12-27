package com.github.gmazzo.codeowners

import org.eclipse.jgit.ignore.FastIgnoreRule
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.nio.file.Files

@CacheableTask
@Suppress("LeakingThis")
abstract class CodeOwnersTask : DefaultTask() {

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    /**
     * Helper input to declare that we only care about paths and not file contents on [rootDirectory] and [sourceFiles]
     *
     * [Incorrect use of the `@Input` annotation](https://docs.gradle.org/7.6/userguide/validation_problems.html#incorrect_use_of_input_annotation)
     */
    @get:Input
    internal val rootDirectoryPath =
        rootDirectory.map { it.asFile.relativeTo(project.rootProject.projectDir) }

    @get:Input
    abstract val codeOwners: Property<CodeOwnersFile>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    abstract val sourceFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        outputDirectory.convention(project.layout.dir(project.provider { temporaryDir }))
    }

    @TaskAction
    fun generateCodeOwnersInfo(): Unit = with(project.layout) {
        val root = rootDirectory.asFile.get()
        val outputDir = outputDirectory.get().apply { asFile.deleteRecursively() }
        val claimed = mutableMapOf<String, List<String>>()

        codeOwners.get().filterIsInstance<CodeOwnersFile.Entry>().reversed().forEach { (pattern, owners) ->
            val ignore = FastIgnoreRule(pattern)

            tailrec fun isClaimed(path: CharSequence): Boolean {
                if (claimed.containsKey(path)) return true

                val index = path.lastIndexOf(File.separatorChar)
                if (index < 0) return claimed.containsKey(".")
                return isClaimed(path.subSequence(0, index))
            }

            sourceFiles.asFileTree.visit {
                val rootPath = it.file.toRelativeString(root)

                if (ignore.isMatch(rootPath, it.isDirectory)) {
                    val targetFile = outputDir
                        .dir(
                            when (it.isDirectory) {
                                true -> "${it.path}/.codeowners"
                                false -> "${it.path}/../${it.file.nameWithoutExtension}.codeowners"
                            }
                        )
                        .asFile
                        .apply { parentFile.mkdirs() }

                    if (!isClaimed(it.path)) {
                        claimed.putIfAbsent(it.path, owners)
                        targetFile.writeText(owners.joinToString(separator = "\n"))
                    }
                }
            }

            // post process the output, moving .codeowners with a single sibling folder up in the hierarchy
            var anotherRound = true
            while (anotherRound) {
                anotherRound = false
                project.fileTree(outputDir) { it.matching { p -> p.include(".codeowners") } }.forEach { file ->
                    file.parentFile.listFiles()!!.singleOrNull { it.name != ".codeowners" }?.let {
                        if (it.isDirectory) {
                            Files.move(file.toPath(), it.toPath().resolve(file.name))
                            anotherRound = true
                        }
                    }
                }
            }
        }
    }

}
