package org.gradle.kotlin.dsl

import io.github.gmazzo.codeowners.CodeOwnersCompilationExtension
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

var KotlinTarget.codeOwners: CodeOwnersCompilationExtension
    get() = (this as ExtensionAware).extensions.getByName<CodeOwnersCompilationExtension>(KotlinTarget::codeOwners.name)
    internal set(value) = (this as ExtensionAware).extensions.add<CodeOwnersCompilationExtension>(KotlinTarget::codeOwners.name, value)

var KotlinCompilation<*>.codeOwners: CodeOwnersCompilationExtension
    get() = (this as ExtensionAware).extensions.getByName<CodeOwnersCompilationExtension>(KotlinCompilation<*>::codeOwners.name)
    internal set(value) = (this as ExtensionAware).extensions.add<CodeOwnersCompilationExtension>(KotlinCompilation<*>::codeOwners.name, value)
