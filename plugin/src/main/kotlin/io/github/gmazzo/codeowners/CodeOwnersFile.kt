package io.github.gmazzo.codeowners

import java.io.Serializable

data class CodeOwnersFile(
    val entries: List<Part>,
) : Serializable, Iterable<CodeOwnersFile.Part> by entries {

    constructor(
        vararg entries: Part,
    ) : this(entries.toList())

    constructor(
        content: String,
    ) : this(content.lineSequence())

    constructor(
        lines: Sequence<String>,
    ) : this(lines.map(Companion::parseLine).toList())

    sealed interface Part : Serializable

    data class Entry(
        val pattern: String,
        val owners: List<String>,
        val comment: String? = null,
    ) : Part {

        constructor(
            pattern: String,
            vararg owners: String,
            comment: String? = null,
        ) : this(pattern, owners.toList(), comment)

    }

    data class Comment(
        val comment: String,
    ) : Part

    object EmptyLine : Part {
        override fun toString() = "EmptyLine"
    }

    private companion object {

        private val lineOrComment = "\\s*+((?!#).+?)?\\s*+(?:(?<!\\\\)#\\s*+(.*?))?\\s*+".toRegex()

        private val patternAndOwners = "(?<!\\\\)\\s+".toRegex()

        fun parseLine(line: String): Part {
            val (_, rawLine, comment) = lineOrComment.matchEntire(line)!!.groupValues

            return when {
                rawLine.isNotBlank() -> rawLine.split(patternAndOwners).let {
                    Entry(pattern = it[0], owners = it.drop(1), comment = comment.takeUnless(String::isBlank))
                }

                comment.isNotBlank() -> Comment(comment)
                else -> EmptyLine
            }
        }

    }

}
