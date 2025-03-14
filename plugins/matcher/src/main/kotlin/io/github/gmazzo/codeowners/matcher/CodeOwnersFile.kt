package io.github.gmazzo.codeowners.matcher

import java.io.Serializable
import kotlin.math.max

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
            val patterns = entries.asSequence()
                .filterIsInstance<Entry>()

            val (indent, commentsIndent) = patterns
                .map { it.pattern.length.tabs(2) to it.owners.lengthOfAll.tabs(1) }
                .fold(0 to 0) { (acc1, acc2), (it1, it2) -> max(acc1, it1) to max(acc2, it2) }

            entries.forEach {
                when (it) {
                    is Entry -> {
                        append(it.pattern)
                        (it.pattern.length until indent).forEach { _ -> append(' ') }
                        it.owners.forEachIndexed { i, owner ->
                            if (i > 0) append(' ')
                            append(owner)
                        }
                        if (it.comment != null) {
                            (it.pattern.length + it.owners.lengthOfAll until indent + commentsIndent)
                                .forEach { _ -> append(' ') }
                            append("# ")
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

        private val Collection<String>.lengthOfAll
            get() = asSequence().map(String::length).max() + size - 1

        private fun Int.tabs(extra: Int) = ((this / 4) + extra) * 4

    }

}
