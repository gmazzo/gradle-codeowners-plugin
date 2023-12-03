package io.github.gmazzo.codeowners

import kotlin.reflect.KClass

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class CodeOwners(vararg val owners: String)

inline fun <reified Type> codeOwnersOf() =
    Type::class.codeOwners

expect val KClass<*>.codeOwners: Set<String>?

expect val Throwable.codeOwners: Set<String>?
