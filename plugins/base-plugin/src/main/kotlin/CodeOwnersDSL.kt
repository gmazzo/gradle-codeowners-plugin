package org.gradle.kotlin.dsl

import io.github.gmazzo.codeowners.CodeOwnersReportTask
import io.github.gmazzo.codeowners.CodeOwnersSourceSet
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskProvider

var CodeOwnersSourceSet.reportTask: TaskProvider<CodeOwnersReportTask>
    get() = (this as ExtensionAware).extensions.getByName<TaskProvider<CodeOwnersReportTask>>(CodeOwnersSourceSet::reportTask.name)
    internal set(value) = (this as ExtensionAware).extensions.add<TaskProvider<CodeOwnersReportTask>>(CodeOwnersSourceSet::reportTask.name, value)
