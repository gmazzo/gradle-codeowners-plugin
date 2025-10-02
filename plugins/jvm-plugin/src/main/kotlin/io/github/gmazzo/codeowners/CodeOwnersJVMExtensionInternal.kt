package io.github.gmazzo.codeowners

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

abstract class CodeOwnersJVMExtensionInternal @Inject constructor(
    project: Project,
    renameTask: Lazy<TaskProvider<CodeOwnersRenameTask>>,
) : CodeOwnersJVMExtension, CodeOwnersExtensionBaseInternal<CodeOwnersJVMSourceSet>(project, renameTask)
