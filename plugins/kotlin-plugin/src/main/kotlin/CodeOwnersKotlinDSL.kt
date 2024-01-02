package org.gradle.kotlin.dsl

import io.github.gmazzo.codeowners.CodeOwnersKotlinSourceSet
import io.github.gmazzo.codeowners.CodeOwnersKotlinTargetExtension
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

var KotlinTarget.codeOwners: CodeOwnersKotlinTargetExtension
    get() = (this as ExtensionAware).extensions.getByName<CodeOwnersKotlinTargetExtension>(KotlinTarget::codeOwners.name)
    internal set(value) = (this as ExtensionAware).extensions.add<CodeOwnersKotlinTargetExtension>(KotlinTarget::codeOwners.name, value)

var KotlinCompilation<*>.codeOwners: CodeOwnersKotlinSourceSet
    get() = (this as ExtensionAware).extensions.getByName<CodeOwnersKotlinSourceSet>(KotlinCompilation<*>::codeOwners.name)
    internal set(value) = (this as ExtensionAware).extensions.add<CodeOwnersKotlinSourceSet>(KotlinCompilation<*>::codeOwners.name, value)

operator fun <Target : CodeOwnersKotlinTargetExtension> Target.invoke(action: Action<Target>) =
    action.execute(this)
