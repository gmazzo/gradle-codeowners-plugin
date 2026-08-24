package io.github.gmazzo.codeowners.compiler

import io.github.gmazzo.codeowners.matcher.CodeOwnersFile
import io.github.gmazzo.codeowners.matcher.CodeOwnersMatcher
import java.io.File

internal class Mappings(
    private val matcher: CodeOwnersMatcher,
    private val mappingFile: File?,
) {

    private var locked = false
    private val mappings = mutableMapOf<File, Mapping>()

    private val unowned = Mapping(owners = emptySet())

    fun resolve(file: File): Mapping? = mappings.computeIfAbsent(file) {
        check(!locked) { "Mappings have already be processed: $file" }
        matcher.ownerOf(file)?.let { Mapping(owners = it) } ?: unowned
    }.takeUnless { it === unowned }

    fun noteFrontedFinished() {
        if (locked) return
        locked = true

        val entries = mappings.entries.asSequence()
            .onEach { (file) -> check(file.isFile) { "$file does not exists!" } }
            .flatMap { (_, mapping) ->
                mapping.classes.asSequence().map {
                    CodeOwnersFile.Entry(pattern = it, mapping.owners.toList())
                }
            }
            .toList()

        mappingFile?.parentFile?.mkdirs()
        mappingFile?.writeText(CodeOwnersFile(entries).content)
    }

    data class Mapping(
        val classes: MutableSet<String> = mutableSetOf(),
        val owners: Set<String>,
    )

}
