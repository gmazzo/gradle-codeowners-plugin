@file:JvmName("CodeOwnersUtils")

package io.github.gmazzo.codeowners

import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

public actual val KClass<*>.codeOwners: Set<String>?
    get() = java.codeOwners

public val Class<*>.codeOwners: Set<String>?
    get() = when {
        Proxy.isProxyClass(this) -> resolveInterfacesIfProxy().mapNotNull { it.codeOwners }.flatten().toSet()
        else -> getAnnotation(CodeOwners::class.java)?.owners?.toSet() ?: enclosingClass?.codeOwners
    }

public val KFunction<*>.codeOwners: Set<String>?
    get() = javaMethod?.declaringClass?.codeOwners

public actual val Throwable.codeOwners: Set<String>?
    get() = getCodeOwners(Thread.currentThread().contextClassLoader)

public fun Throwable.getCodeOwners(classLoader: ClassLoader): Set<String>? =
    stackTrace.asSequence().mapNotNull { it.getCodeOwners(classLoader) }.firstOrNull()

private fun StackTraceElement.getCodeOwners(classLoader: ClassLoader) = try {
    Class.forName(className, false, classLoader).codeOwners

} catch (e: Throwable) {
    null
}

private fun Class<*>.resolveInterfacesIfProxy() = when {
    !Proxy.isProxyClass(this) -> sequenceOf(this)
    else -> interfaces.asSequence()
}
