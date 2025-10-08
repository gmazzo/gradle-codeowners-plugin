package io.github.gmazzo.codeowners

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

internal abstract class CodeOwnersExtensionInternal @Inject constructor(
    project: Project,
    renameTask: Lazy<TaskProvider<CodeOwnersRenameTask>>,
) : CodeOwnersExtension,
    CodeOwnersExtensionBaseInternal<CodeOwnersSourceSet>(project, renameTask)
