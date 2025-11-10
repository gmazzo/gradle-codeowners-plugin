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

public fun Throwable.getCodeOwners(classLoader: ClassLoader): Set<String>? {
    try {
        for (element in stackTrace) {
            val owners = Class.forName(element.className, false, classLoader).codeOwners

            if (owners != null) {
                return owners
            }
        }

    } catch (_: Exception) {
        /**
         * `Class.forName` produces a crash, which its `fillInStackTrace` will be an expensive operation.
         *
         * Since we don't expect to have an unresolvable class in the stack trace, given the stack trace should come
         * from a valid execution path, keep trying to resolve the remaining class names once 1 has failed would be
         * probably lead to a system resources starvation (and an ANR on Android, which is why we are doing this change).
         *
         * We just assume a panic condition instead, and abort the resolution.
         */
    }
    return null
}

private fun Class<*>.resolveInterfacesIfProxy() = when {
    !Proxy.isProxyClass(this) -> sequenceOf(this)
    else -> interfaces.asSequence()
}
