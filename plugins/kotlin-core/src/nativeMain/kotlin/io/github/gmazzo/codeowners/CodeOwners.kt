@file:OptIn(ExperimentalAssociatedObjects::class)

package io.github.gmazzo.codeowners

import kotlin.reflect.AssociatedObjectKey
import kotlin.reflect.ExperimentalAssociatedObjects
import kotlin.reflect.KClass
import kotlin.reflect.findAssociatedObject

internal abstract class CodeOwnersProvider(val value: Array<String>)

@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
internal annotation class CodeOwnersProviderKey(val provider: KClass<out CodeOwnersProvider>)

actual val KClass<*>.codeOwners: Set<String>?
    get() = (findAssociatedObject<CodeOwnersProviderKey>() as CodeOwnersProvider?)?.value?.toSet()

actual val Throwable.codeOwners: Set<String>?
    get() = this::class.codeOwners
