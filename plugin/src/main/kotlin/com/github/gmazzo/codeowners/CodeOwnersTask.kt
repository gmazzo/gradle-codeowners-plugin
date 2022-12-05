package com.github.gmazzo.codeowners

import org.eclipse.jgit.ignore.FastIgnoreRule
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
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
    abstract val sourceDirectories: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        outputDirectory.convention(project.layout.dir(project.provider { temporaryDir }))
    }

    @TaskAction
    fun generatePackagesInfo(): Unit = with(project.layout) {
        val root = codeOwnersRoot.asFile.get()
        val outputDir = outputDirectory.get().apply { asFile.deleteRecursively() }
        val claimed = mutableMapOf<String, List<String>>()

        codeOwners.get().filterIsInstance<CodeOwnersFile.Entry>().reversed().forEach { (pattern, owners) ->
            val ignore = FastIgnoreRule(pattern)

            tailrec fun isClaimed(path: CharSequence): Boolean {
                if (claimed.containsKey(path)) return true

                val index = path.lastIndexOf(File.separatorChar)
                if (index < 0) return false
                return isClaimed(path.subSequence(0, index))
            }

            sourceDirectories.asFileTree.visit {
                if (it.file.path.startsWith(outputDir.asFile.path)) {
                    // our output directory will part of sources too!
                    return@visit
                }

                val rootPath = it.file.toRelativeString(root)

                // TODO how to handle files?
                if (it.isDirectory && ignore.isMatch(rootPath, it.isDirectory) && !isClaimed(it.path)) {
                    claimed.putIfAbsent(it.path, owners)
                    writePackageInfo(it.path, owners, outputDir)
                }
            }
        }
    }

    private fun writePackageInfo(path: String, owners: List<String>, outputDir: Directory) {
        val packageName = path.replace('/', '.')

        outputDir.file("$path/package-info.java").asFile.apply {
            parentFile.mkdirs()
            writeText(
                """
                @${CodeOwner::class.java.name}(${owners.joinToString(separator = ", ") { "\"$it\"" }})
                package $packageName;
                """.trimIndent()
            )
        }
    }

}
