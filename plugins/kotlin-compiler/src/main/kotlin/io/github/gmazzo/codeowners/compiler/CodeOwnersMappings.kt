package io.github.gmazzo.codeowners.compiler

import io.github.gmazzo.codeowners.matcher.CodeOwnersFile
import io.github.gmazzo.codeowners.matcher.CodeOwnersMatcher
import java.io.File

class CodeOwnersMappings(
    private val matcher: CodeOwnersMatcher,
    private var mappingFile: File?,
) {

    private val mappings = mutableMapOf<File, Mapping?>()

    fun resolve(file: File) = mappings.computeIfAbsent(file) {
        matcher.ownerOf(file)?.let { Mapping(owners = it) }
    }

    fun noteFrontedFinished() = mappingFile?.let { file ->
        mappingFile = null

        val entries = mappings.entries.asSequence()
            .onEach { (file) -> check(file.isFile) { "$file does not exists!" } }
            .filter { it.value != null }
            .flatMap { (_, mapping) ->
                mapping!!.classes.asSequence().map {
                    CodeOwnersFile.Entry(pattern = it, mapping.owners.toList())
                }
            }
            .toList()

        file.parentFile?.mkdirs()
        file.writeText(CodeOwnersFile(entries).content)
    }

    data class Mapping(
        val classes: MutableSet<String> = mutableSetOf(),
        val owners: Set<String>,
    )

}
