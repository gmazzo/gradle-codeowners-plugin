package com.github.gmazzo.codeowners

import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test

class CodeOwnersFileTest {

    @Test
    fun `should handle paths with spaces`() {
        testParse(
            "path\\\\ with\\\\ spaces/ @space-owner",
            CodeOwnersFile(CodeOwnersFile.Entry("path\\\\ with\\\\ spaces/", "@space-owner")),
        )
    }

    @Test
    fun `should handle comments`() {
        testParse(
            " # This is a comment ",
            CodeOwnersFile(CodeOwnersFile.Comment("This is a comment")),
        )
    }

    @Test
    fun `should handle inline comments`() {
        testParse(
            "/foo/bar    @foo @bar   #  This is an inline comment. ",
            CodeOwnersFile(
                CodeOwnersFile.Entry("/foo/bar", "@foo", "@bar", comment = "This is an inline comment.")
            ),
        )
    }

    @Test
    fun `should handle paths slash# sequences`() {
        testParse(
            "\\\\#foo @foo-owner",
            CodeOwnersFile(CodeOwnersFile.Entry("\\\\#foo", "@foo-owner")),
        )
    }

    @Test
    fun `should parse a complex file`() {
        testParse(
            """
            # This is a comment.
            
            *       @global-owner1 @global-owner2
            *.js    @js-owner #This is an inline comment. 
            *.go docs@example.com
            /build/logs/ @doctocat
            #A final comment
            
        """.trimIndent(),
            CodeOwnersFile(
                CodeOwnersFile.Comment("This is a comment."),
                CodeOwnersFile.Entry("*", "@global-owner1", "@global-owner2"),
                CodeOwnersFile.Entry("*.js", "@js-owner", comment = "This is an inline comment."),
                CodeOwnersFile.Entry("*.go", "docs@example.com"),
                CodeOwnersFile.Entry("/build/logs/", "@doctocat"),
                CodeOwnersFile.Comment("A final comment"),
            ),
        )
    }

    private fun testParse(content: String, expected: CodeOwnersFile) {
        val parsed = CodeOwnersFile(content)

        assertIterableEquals(expected, parsed)
    }

}
