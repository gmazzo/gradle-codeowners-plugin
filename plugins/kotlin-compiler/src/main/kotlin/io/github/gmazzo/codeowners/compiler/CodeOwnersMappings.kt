package io.github.gmazzo.codeowners.compiler

import io.github.gmazzo.codeowners.matcher.CodeOwnersFile
import io.github.gmazzo.codeowners.matcher.CodeOwnersMatcher
import java.io.File

public class CodeOwnersMappings(
    private val matcher: CodeOwnersMatcher,
    private var mappingFile: File?,
) {

    private val mappings = mutableMapOf<File, Mapping?>()

    public fun resolve(file: File): CodeOwnersMappings.Mapping? = mappings.computeIfAbsent(file) {
        matcher.ownerOf(file)?.let { Mapping(owners = it) }
    }

    public fun noteFrontedFinished(): Unit? = mappingFile?.let { file ->
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

    public data class Mapping(
        val classes: MutableSet<String> = mutableSetOf(),
        val owners: Set<String>,
    )

}
