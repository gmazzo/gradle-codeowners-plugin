package org.gradle.kotlin.dsl

import io.github.gmazzo.codeowners.CodeOwnersKotlinSourceSet
import io.github.gmazzo.codeowners.CodeOwnersKotlinTargetExtension
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

public var KotlinTarget.codeOwners: CodeOwnersKotlinTargetExtension
    get() = (this as ExtensionAware).extensions.getByName<CodeOwnersKotlinTargetExtension>(KotlinTarget::codeOwners.name)
    internal set(value) = (this as ExtensionAware).extensions.add<CodeOwnersKotlinTargetExtension>(
        KotlinTarget::codeOwners.name,
        value
    )

public var KotlinCompilation<*>.codeOwners: CodeOwnersKotlinSourceSet
    get() = extensions.getByName<CodeOwnersKotlinSourceSet>(KotlinCompilation<*>::codeOwners.name)
    internal set(value) = extensions.add<CodeOwnersKotlinSourceSet>(
        KotlinCompilation<*>::codeOwners.name,
        value
    )

/**
 * workaround of AGP's `KotlinMultiplatformAndroidCompilationImpl` not getting instantiated with Gradle
 * therefore, it does not implements `ExtensionAware`. This may change in the future when it's gets mature
  */
private val KotlinCompilation<*>.extensions: ExtensionContainer
    get() = when (this) {
        is ExtensionAware -> extensions
        else -> with((target as ExtensionAware).extensions) {
            val helperExtensionName = "${this@extensions.name}_extensions"

            findByName(helperExtensionName) as ExtensionContainer?
                ?: project.objects.newInstance<AdHocExtensions>().extensions
                    .also { add(helperExtensionName, it) }
        }
    }

public operator fun <Target : CodeOwnersKotlinTargetExtension> Target.invoke(action: Action<Target>): Unit =
    action.execute(this)

private interface AdHocExtensions : ExtensionAware
