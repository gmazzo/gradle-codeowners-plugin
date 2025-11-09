@file:JvmName("CodeOwnersUtils")

package io.github.gmazzo.codeowners

import java.io.Reader
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

public inline fun <reified Type> codeOwnersOf(): Set<String>? =
    Type::class.codeOwners

public val KClass<*>.codeOwners: Set<String>?
    get() = java.codeOwners

public val Class<*>.codeOwners: Set<String>?
    get() = with(topLevelClass()) { classLoader.getCodeOwners(`package`?.name, simpleName) }

public val Throwable.codeOwners: Set<String>?
    get() = stackTrace.asSequence().mapNotNull { it.codeOwners }.firstOrNull()

private tailrec fun Class<*>.topLevelClass(): Class<*> = when (val enclosing = enclosingClass) {
    null -> when {
        Proxy.isProxyClass(this) -> interfaces.firstOrNull() ?: this
        else -> this
    }

    else -> enclosing.topLevelClass()
}

public val StackTraceElement.codeOwners: Set<String>?
    get() {
        val clazz = runCatching { Class.forName(className) }.getOrNull() ?: return null

        return fileName?.substringBeforeLast('.')
            ?.let { clazz.classLoader.getCodeOwners(clazz.`package`.name, it) }
            ?: clazz.codeOwners
    }

@JvmOverloads
public tailrec fun ClassLoader.getCodeOwners(packageName: String?, className: String? = null): Set<String>? {
    val packagePath = packageName?.replace('.', '/') ?: return null
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
