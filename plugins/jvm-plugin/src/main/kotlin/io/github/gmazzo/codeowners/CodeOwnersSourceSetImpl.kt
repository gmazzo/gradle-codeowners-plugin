package io.github.gmazzo.codeowners

import org.gradle.api.Named
import org.gradle.api.tasks.TaskProvider
import javax.inject.Inject

internal abstract class CodeOwnersSourceSetImpl @Inject constructor(
    name: String,
    override val generateTask: TaskProvider<CodeOwnersTask>,
) : CodeOwnersSourceSet,
    Named by (Named { name })
