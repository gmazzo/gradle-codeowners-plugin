@file:JvmName("CodeOwners")

package io.github.gmazzo.codeowners

import java.io.Reader
import kotlin.reflect.KClass

inline fun <reified Type> codeOwnersOf() =
    Type::class.codeOwners

val KClass<*>.codeOwners
    get() = java.codeOwners

val Class<*>.codeOwners: Set<String>?
    get() = with(topLevelClass()) { classLoader.getCodeOwners(`package`.name, simpleName) }

val Throwable.codeOwners
    get() = stackTrace.asSequence().map { it.codeOwners }.firstOrNull()

private tailrec fun Class<*>.topLevelClass(): Class<*> = when (val enclosing = enclosingClass) {
    null -> this
    else -> enclosing.topLevelClass()
}

private val StackTraceElement.codeOwners: Set<String>?
    get() {
        val clazz = runCatching { Class.forName(className) }.getOrNull() ?: return null

        return fileName?.substringBeforeLast('.')
            ?.let { clazz.classLoader.getCodeOwners(clazz.`package`.name, it) }
            ?: clazz.codeOwners
    }

@JvmOverloads
tailrec fun ClassLoader.getCodeOwners(packageName: String, className: String? = null): Set<String>? {
    val packagePath = packageName.replace('.', '/')
    val path = "$packagePath/${className.orEmpty()}.codeowners"
    val owners = getResources(path).asSequence()
        .flatMap { it.openStream()?.reader()?.use(Reader::readLines).orEmpty() }
        .toSet()

    if (owners.isNotEmpty()) return owners
    if (className != null) return getCodeOwners(packageName, null)
    val index = packagePath.lastIndexOf('/')
    if (index < 0) return null
    return getCodeOwners(packagePath.substring(0, index), null)
}
