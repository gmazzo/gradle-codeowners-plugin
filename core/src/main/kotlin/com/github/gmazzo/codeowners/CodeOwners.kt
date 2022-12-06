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
