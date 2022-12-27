@file:JvmName("CodeOwners")
package com.github.gmazzo.codeowners

import java.io.Reader
import kotlin.reflect.KClass

inline fun <reified Type> codeOwnersOf() =
    Type::class.codeOwners

val KClass<*>.codeOwners
    get() = java.codeOwners

val Class<*>.codeOwners: List<String>?
    get() = with(topLevelClass()) {
        codeOwners(simpleName) ?: recursiveCodeOwners(packageName.replace('.', '/'))
    }

val Throwable.codeOwners
    get() = stackTrace.asSequence().codeOwners

val Sequence<StackTraceElement>.codeOwners: List<String>?
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
    getResource("$path.codeowners")?.openStream()?.reader()?.use(Reader::readLines)

private tailrec fun Class<*>.recursiveCodeOwners(packagePath: String): List<String>? {
    val owners = codeOwners("/$packagePath/")
    if (owners != null) return owners

    val index = packagePath.lastIndexOf('/')
    if (index < 0) return null
    return recursiveCodeOwners(packagePath.substring(0, index))
}
