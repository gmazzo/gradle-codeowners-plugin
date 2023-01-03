@file:JvmName("CodeOwners")

package com.github.gmazzo.codeowners

import java.io.Reader
import java.net.URL
import kotlin.reflect.KClass

inline fun <reified Type> codeOwnersOf() =
    Type::class.codeOwners

val KClass<*>.codeOwners
    get() = java.codeOwners

val Class<*>.codeOwners: Set<String>?
    get() = with(topLevelClass()) {
        codeOwners(simpleName) ?: recursiveCodeOwners(packageName.replace('.', '/'))
    }

val Throwable.codeOwners
    get() = stackTrace.asSequence().codeOwners

val Sequence<StackTraceElement>.codeOwners: Set<String>?
    get() = mapNotNull { st ->
        when (val clazz = runCatching { Class.forName(st.className) }.getOrNull()) {
            null -> null
            else -> st.fileName?.substringBeforeLast('.')?.let(clazz::codeOwners) ?: clazz.codeOwners
        }
    }.firstOrNull()

private tailrec fun Class<*>.topLevelClass(): Class<*> = when (val enclosing = enclosingClass) {
    null -> this
    else -> enclosing.topLevelClass()
}

private fun Class<*>.codeOwners(path: String) =
    getResource("$path.codeowners")?.codeOwners

private val URL.codeOwners
    get() = openStream()?.reader()?.use(Reader::readLines)?.toSet()

private tailrec fun Class<*>.recursiveCodeOwners(packagePath: String): Set<String>? {
    val owners = classLoader.getResources("$packagePath/.codeowners").asSequence()
        .flatMap { it.codeOwners.orEmpty() }
        .toSet()

    if (owners.isNotEmpty()) return owners
    val index = packagePath.lastIndexOf('/')
    if (index < 0) return null
    return recursiveCodeOwners(packagePath.substring(0, index))
}
