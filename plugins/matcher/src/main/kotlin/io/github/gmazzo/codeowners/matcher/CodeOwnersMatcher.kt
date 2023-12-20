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

    fun ownerOf(file: File, isDirectory: Boolean = file.isDirectory): Set<String>? =
        ownerOf(file.toRelativeString(rootDir), isDirectory)

    fun ownerOf(relativePath: String, isDirectory: Boolean = relativePath.endsWith('/')): Set<String>? =
        matchers.find { (_, ignore) -> ignore.isMatch(relativePath, isDirectory) }?.first

}
