package io.github.gmazzo.codeowners

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

abstract class CodeOwnersKotlinExtensionInternal @Inject constructor(
    project: Project,
    renameTask: Lazy<TaskProvider<CodeOwnersRenameTask>>,
) : CodeOwnersKotlinExtension, CodeOwnersExtensionBaseInternal<CodeOwnersKotlinSourceSet>(project, renameTask)
