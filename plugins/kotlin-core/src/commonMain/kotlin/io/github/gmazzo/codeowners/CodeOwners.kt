package io.github.gmazzo.codeowners

import kotlin.reflect.KClass

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
public annotation class CodeOwners(vararg val owners: String)

public inline fun <reified Type> codeOwnersOf(): Set<String>? =
    Type::class.codeOwners

public expect val KClass<*>.codeOwners: Set<String>?

public expect val Throwable.codeOwners: Set<String>?
