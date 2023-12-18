package org.gradle.kotlin.dsl

import com.android.build.api.variant.Component
import com.android.build.api.variant.Variant
import io.github.gmazzo.codeowners.CodeOwnersSourceSet
import org.gradle.api.plugins.ExtensionAware

var Component.codeOwners: CodeOwnersSourceSet
    get() = (this as ExtensionAware).extensions.getByName<CodeOwnersSourceSet>(Variant::codeOwners.name)
    internal set(value) = (this as ExtensionAware).extensions.add<CodeOwnersSourceSet>(Variant::codeOwners.name, value)
