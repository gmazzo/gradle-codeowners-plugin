package org.gradle.kotlin.dsl

import com.android.build.api.variant.Component
import io.github.gmazzo.codeowners.CodeOwnersJVMSourceSet
import io.github.gmazzo.codeowners.CodeOwnersResourcesTask
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskProvider

var Component.codeOwners: CodeOwnersJVMSourceSet
    get() = (this as ExtensionAware).extensions.getByName<CodeOwnersJVMSourceSet>(Component::codeOwners.name)
    internal set(value) = (this as ExtensionAware).extensions.add<CodeOwnersJVMSourceSet>(
        Component::codeOwners.name,
        value
    )

var CodeOwnersJVMSourceSet.generateTask: TaskProvider<CodeOwnersResourcesTask>
    get() = (this as ExtensionAware).extensions.getByName<TaskProvider<CodeOwnersResourcesTask>>(CodeOwnersJVMSourceSet::generateTask.name)
    internal set(value) = (this as ExtensionAware).extensions.add<TaskProvider<CodeOwnersResourcesTask>>(
        CodeOwnersJVMSourceSet::generateTask.name,
        value
    )
