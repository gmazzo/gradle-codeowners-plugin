package io.github.gmazzo.codeowners.matcher

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import kotlin.test.assertEquals

class CodeOwnersMatcherTest {

    private val codeOwnersFile = CodeOwnersFile(
        CodeOwnersFile.Comment("This is a comment."),
        CodeOwnersFile.EmptyLine,
        CodeOwnersFile.Entry("*", "@global-owner1", "@global-owner2"),
        CodeOwnersFile.Entry("*.js", "@js-owner", comment = "This is an inline comment."),
        CodeOwnersFile.Entry("*.go", "docs@example.com"),
        CodeOwnersFile.Entry("/build/logs/", "@doctocat"),
        CodeOwnersFile.Comment("A final comment"),
        CodeOwnersFile.EmptyLine,
    )

    private val matcher by lazy {
        CodeOwnersMatcher(File("root"), codeOwnersFile)
    }

    @ParameterizedTest
    @CsvSource(
        "file,@global-owner1 @global-owner2",
        "doc/file,@global-owner1 @global-owner2",
        "doc/file,@global-owner1 @global-owner2",
        "/build/logs/file,@doctocat",
        "/build/logs/file.js,@doctocat",
        "doc/file.js,@js-owner",
    )
    fun `should resolve by relative path`(relativePath: String, expectedOwners: String) {
        assertEquals(expectedOwners.splitToSequence(' ').toSet(), matcher.ownerOf(relativePath))
    }

    @ParameterizedTest
    @CsvSource(
        "root/file,@global-owner1 @global-owner2",
        "root/doc/file,@global-owner1 @global-owner2",
        "root/doc/file,@global-owner1 @global-owner2",
        "root//build/logs/file,@doctocat",
        "root//build/logs/file.js,@doctocat",
        "root/doc/file.js,@js-owner",
    )
    fun `should resolve by file`(file: File, expectedOwners: String) {
        assertEquals(expectedOwners.splitToSequence(' ').toSet(), matcher.ownerOf(file))
    }

}
