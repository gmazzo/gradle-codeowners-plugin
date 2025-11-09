package org.gradle.kotlin.dsl

import io.github.gmazzo.codeowners.CodeOwnersReportTask
import io.github.gmazzo.codeowners.CodeOwnersSourceSet
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskProvider

public var CodeOwnersSourceSet.reportTask: TaskProvider<CodeOwnersReportTask>
    get() = (this as ExtensionAware).extensions.getByName<TaskProvider<CodeOwnersReportTask>>(CodeOwnersSourceSet::reportTask.name)
    internal set(value) = (this as ExtensionAware).extensions.add<TaskProvider<CodeOwnersReportTask>>(
        CodeOwnersSourceSet::reportTask.name,
        value
    )

public operator fun <SourceSet : CodeOwnersSourceSet> SourceSet.invoke(action: Action<SourceSet>): Unit =
    action.execute(this)
