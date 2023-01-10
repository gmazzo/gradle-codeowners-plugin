package io.github.gmazzo.codeowners

import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.TaskProvider
import javax.inject.Inject

internal abstract class CodeOwnersSourceSetImpl @Inject constructor(
    sources: SourceDirectorySet,
    override val generateTask: TaskProvider<CodeOwnersTask>,
) : CodeOwnersSourceSet,
    SourceDirectorySet by sources
