@file:JvmName("CodeOwnersUtils")

package io.github.gmazzo.codeowners

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

actual val KClass<*>.codeOwners: Set<String>?
    get() = java.codeOwners

val Class<*>.codeOwners: Set<String>?
    get() = getAnnotation(CodeOwners::class.java)?.owners?.toSet()

val KFunction<*>.codeOwners: Set<String>?
    get() = javaMethod?.declaringClass?.codeOwners

actual val Throwable.codeOwners
    get() = getCodeOwners(Thread.currentThread().contextClassLoader)

fun Throwable.getCodeOwners(classLoader: ClassLoader) =
    stackTrace.asSequence().mapNotNull { it.getCodeOwners(classLoader) }.firstOrNull()

private fun StackTraceElement.getCodeOwners(classLoader: ClassLoader) = try {
    Class.forName(className, false, classLoader).codeOwners

} catch (e: Throwable) {
    null
}
