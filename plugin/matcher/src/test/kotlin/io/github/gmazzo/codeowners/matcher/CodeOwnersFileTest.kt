package io.github.gmazzo.codeowners.matcher

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

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
                CodeOwnersFile.EmptyLine,
                CodeOwnersFile.Entry("*", "@global-owner1", "@global-owner2"),
                CodeOwnersFile.Entry("*.js", "@js-owner", comment = "This is an inline comment."),
                CodeOwnersFile.Entry("*.go", "docs@example.com"),
                CodeOwnersFile.Entry("/build/logs/", "@doctocat"),
                CodeOwnersFile.Comment("A final comment"),
                CodeOwnersFile.EmptyLine,
            ),
        )
    }

    @Test
    fun `when file has entry without owners, should parse it anyway`() {
        testParse(
            """
            # This is a comment.
            
            entryWithoutOwners
            
            *       @global-owner1 @global-owner2
            *.js    @js-owner #This is an inline comment.
        """.trimIndent(),
            CodeOwnersFile(
                CodeOwnersFile.Comment("This is a comment."),
                CodeOwnersFile.EmptyLine,
                CodeOwnersFile.Entry("entryWithoutOwners"),
                CodeOwnersFile.EmptyLine,
                CodeOwnersFile.Entry("*", "@global-owner1", "@global-owner2"),
                CodeOwnersFile.Entry("*.js", "@js-owner", comment = "This is an inline comment."),
            ),
        )
    }

    private fun testParse(content: String, expected: CodeOwnersFile) {
        val parsed = CodeOwnersFile(content)

        assertEquals(expected, parsed)
    }

    @Test
    fun `given entries, should generate codeowners content`() {
        val codeOwners = CodeOwnersFile(
            CodeOwnersFile.Comment("This is a comment."),
            CodeOwnersFile.EmptyLine,
            CodeOwnersFile.Entry("*", "@global-owner1", "@global-owner2"),
            CodeOwnersFile.Entry("*.js", "@js-owner", comment = "This is an inline comment."),
            CodeOwnersFile.Entry("*.go", "docs@example.com"),
            CodeOwnersFile.Entry("/build/logs/", "@doctocat"),
            CodeOwnersFile.Comment("A final comment"),
            CodeOwnersFile.EmptyLine,
        )

        assertEquals("""
            # This is a comment.
            
            *                   @global-owner1 @global-owner2
            *.js                @js-owner                           # This is an inline comment.
            *.go                docs@example.com
            /build/logs/        @doctocat
            # A final comment
            
            
        """.trimIndent(), codeOwners.content)
    }

    @Test
    fun `entries can be serialized correctly`() {
        val original = CodeOwnersFile(
            CodeOwnersFile.Comment("This is a comment."),
            CodeOwnersFile.EmptyLine,
            CodeOwnersFile.Entry("*", "@global-owner1", "@global-owner2"),
            CodeOwnersFile.Entry("*.js", "@js-owner", comment = "This is an inline comment."),
            CodeOwnersFile.Entry("*.go", "docs@example.com"),
            CodeOwnersFile.Entry("/build/logs/", "@doctocat"),
            CodeOwnersFile.Comment("A final comment"),
            CodeOwnersFile.EmptyLine,
        )

        val reSerialized = ObjectInputStream(ByteArrayInputStream(ByteArrayOutputStream()
            .use { ObjectOutputStream(it).use { out -> out.writeObject(original) }; it.toByteArray() }))
            .readObject() as CodeOwnersFile

        assertEquals(original, reSerialized)
        assertEquals(original.content, reSerialized.content)
    }

}
