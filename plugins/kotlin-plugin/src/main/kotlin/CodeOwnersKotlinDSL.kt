package org.gradle.kotlin.dsl

import io.github.gmazzo.codeowners.CodeOwnersKotlinCompilationExtension
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

var KotlinTarget.codeOwners: CodeOwnersKotlinCompilationExtension
    get() = (this as ExtensionAware).extensions.getByName<CodeOwnersKotlinCompilationExtension>(KotlinTarget::codeOwners.name)
    internal set(value) = (this as ExtensionAware).extensions.add<CodeOwnersKotlinCompilationExtension>(KotlinTarget::codeOwners.name, value)

var KotlinCompilation<*>.codeOwners: CodeOwnersKotlinCompilationExtension
    get() = (this as ExtensionAware).extensions.getByName<CodeOwnersKotlinCompilationExtension>(KotlinCompilation<*>::codeOwners.name)
    internal set(value) = (this as ExtensionAware).extensions.add<CodeOwnersKotlinCompilationExtension>(KotlinCompilation<*>::codeOwners.name, value)
