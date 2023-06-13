package io.github.gmazzo.codeowners

import java.io.Serializable

data class CodeOwnersFile(
    val entries: List<Part>,
) : Serializable, Iterable<CodeOwnersFile.Part> by entries {

    val content: String by lazy { generateContent(entries) }

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
        override fun equals(other: Any?) = other is EmptyLine
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

        fun generateContent(entries: List<Part>) = buildString {
            val indent = entries.asSequence()
                .filterIsInstance<Entry>()
                .map { it.pattern.length }
                .max()
                .let { ((it / 4) + 2) * 4 }

            entries.forEach {
                when(it) {
                    is Entry -> {
                        append(it.pattern)
                        (it.pattern.length until indent).forEach { _ -> append(' ') }
                        it.owners.forEachIndexed { i, owner ->
                            if (i > 0) append(' ')
                            append(owner)
                        }
                        if (it.comment != null) {
                            append(" # ")
                            append(it.comment)
                        }
                        appendLine()
                    }
                    is Comment -> {
                        append("# ")
                        appendLine(it.comment)
                    }
                    is EmptyLine -> appendLine()
                }
            }
        }

    }

}
