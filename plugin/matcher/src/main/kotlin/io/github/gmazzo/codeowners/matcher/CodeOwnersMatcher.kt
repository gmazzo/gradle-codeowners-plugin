package io.github.gmazzo.codeowners.matcher

import org.eclipse.jgit.ignore.FastIgnoreRule
import java.io.File

class CodeOwnersMatcher(
    private val rootDir: File,
    codeOwners: CodeOwnersFile,
) {

    private val matchers = codeOwners
        .filterIsInstance<CodeOwnersFile.Entry>()
        .reversed()
        .map { it.owners.toSet() to FastIgnoreRule(it.pattern) }

    fun ownerOf(file: File): Set<String>? = matchers.find { (_, ignore) ->
        val path = file.toRelativeString(rootDir)

        ignore.isMatch(path, path.endsWith('/'))
    }?.first

}
