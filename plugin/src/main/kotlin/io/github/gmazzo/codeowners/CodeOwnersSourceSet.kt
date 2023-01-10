package io.github.gmazzo.codeowners

import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider

interface CodeOwnersSourceSet : SourceDirectorySet {
    val includeAsResources: Property<Boolean>
    val includeCoreDependency: Property<Boolean>
    val generateTask: TaskProvider<CodeOwnersTask>
}
