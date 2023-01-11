package io.github.gmazzo.codeowners

import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider

interface CodeOwnersSourceSet : SourceDirectorySet {

    /**
     * If it should compute the code owners for this source set
     */
    val enabled: Property<Boolean>

    /**
     * The task used to generate this source set code owners resources
     */
    val generateTask: TaskProvider<CodeOwnersTask>

}
