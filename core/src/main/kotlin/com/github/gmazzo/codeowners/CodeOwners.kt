package com.github.gmazzo.codeowners

import kotlin.reflect.KClass

inline fun <reified Type> codeOwnerOf() =
    Type::class.codeOwner

inline val KClass<*>.codeOwner
    get() = java.codeOwner

inline val Class<*>.codeOwner
    get() = getAnnotation(CodeOwner::class.java)?.value ?: `package`.codeOwner(classLoader)

fun Package.codeOwner(
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader
): Array<out String>? = generateSequence(this) { it.parent(classLoader) }
    .mapNotNull { it.getAnnotation(CodeOwner::class.java)?.value }
    .firstOrNull()

private fun Package.parent(classLoader: ClassLoader) = when (val i = name.lastIndexOf('.')) {
    -1 -> null
    else -> classLoader.getDefinedPackage(name.substring(0, i))
}
