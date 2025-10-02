package io.github.gmazzo.codeowners

import io.github.gmazzo.codeowners.matcher.CodeOwnersFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class CodeOwnersRenameTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val codeOwnersFile: RegularFileProperty

    @get:Input
    abstract val codeOwnersRenamer: Property<CodeOwnersExtensionBase.Renamer>

    @get:OutputFile
    abstract val renamedCodeOwnersFile: RegularFileProperty

    init {
        renamedCodeOwnersFile.convention(project.layout.file(codeOwnersFile.asFile.map {
            temporaryDir.resolve("${it.name}.renamed")
        }))
    }

    @TaskAction
    fun renameCodeOwners() {
        val renamer = codeOwnersRenamer.get()
        val original = codeOwnersFile.asFile.get().useLines { CodeOwnersFile(it) }
        val renamed = CodeOwnersFile(original.map { entry ->
            when (entry) {
                is CodeOwnersFile.Entry -> CodeOwnersFile.Entry(
                    pattern = entry.pattern,
                    owners = entry.owners.map(renamer::rename),
                    comment = entry.comment)
                else -> entry
            }
        })

        with(renamedCodeOwnersFile.asFile.get()) {
            parentFile?.mkdirs()
            writeText(renamed.content)
        }
    }

}
