package org.gradle.kotlin.dsl

import com.android.build.api.variant.Component
import io.github.gmazzo.codeowners.CodeOwnersJVMSourceSet
import io.github.gmazzo.codeowners.CodeOwnersTask
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskProvider

var Component.codeOwners: CodeOwnersJVMSourceSet
    get() = (this as ExtensionAware).extensions.getByName<CodeOwnersJVMSourceSet>(Component::codeOwners.name)
    internal set(value) = (this as ExtensionAware).extensions.add<CodeOwnersJVMSourceSet>(Component::codeOwners.name, value)

var CodeOwnersJVMSourceSet.generateTask: TaskProvider<CodeOwnersTask>
    get() = (this as ExtensionAware).extensions.getByName<TaskProvider<CodeOwnersTask>>(CodeOwnersJVMSourceSet::generateTask.name)
    internal set(value) = (this as ExtensionAware).extensions.add<TaskProvider<CodeOwnersTask>>(CodeOwnersJVMSourceSet::generateTask.name, value)
