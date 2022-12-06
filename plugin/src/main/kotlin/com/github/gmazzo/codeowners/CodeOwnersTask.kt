package com.github.gmazzo.codeowners

import org.eclipse.jgit.ignore.FastIgnoreRule
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

@Suppress("LeakingThis")
abstract class CodeOwnersTask : DefaultTask() {

    @get:Input
    abstract val codeOwnersRoot: DirectoryProperty

    @get:Input
    abstract val codeOwners: Property<CodeOwnersFile>

    @get:Input
    val sourceDirectories: SourceDirectorySet = project.objects
        .sourceDirectorySet("sourceDirectories", "source directories")

    @get:OutputDirectory
    val outputDirectory: DirectoryProperty = sourceDirectories.destinationDirectory
        .convention(project.layout.dir(project.provider { temporaryDir }))

    @TaskAction
    fun generateCodeOwnersInfo(): Unit = with(project.layout) {
        val root = codeOwnersRoot.asFile.get()
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

            fun process(file: File, relativePath: String, isDirectory: Boolean) {
                val rootPath = file.toRelativeString(root)

                if (ignore.isMatch(rootPath, isDirectory) && !isClaimed(relativePath)) {
                    claimed.putIfAbsent(relativePath, owners)

                    outputDir.dir(
                        when (isDirectory) {
                            true -> "$relativePath/.codeowners"
                            false -> "$relativePath/../${file.nameWithoutExtension}.codeowners"
                        }
                    ).asFile.apply {
                        parentFile.mkdirs()
                        writeText(owners.joinToString(separator = "\n"))
                    }
                }
            }

            sourceDirectories.srcDirs.forEach {
                if (it.isDirectory) {
                    process(it, ".", true)
                }
            }
            sourceDirectories.asFileTree.visit { process(it.file, it.path, it.isDirectory) }
        }
    }

}
