package io.github.gmazzo.codeowners

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.TaskProvider

public abstract class CodeOwnersExtensionBaseInternal<SourceSet : CodeOwnersSourceSet>(
    private val project: Project,
    public val renameTask: Lazy<TaskProvider<CodeOwnersRenameTask>>,
) : CodeOwnersExtensionBase<SourceSet> {

    public abstract val renamedCodeOwnersFile: RegularFileProperty

    /**
     * A hack to avoid "can't query property of task before it has been run" on native kotlin compilations
     */
    public abstract val renamedCodeOwnersFileUntracked: RegularFileProperty

    override fun codeOwnersRenamer(renamer: CodeOwnersExtensionBase.Renamer) {
        if (!renameTask.isInitialized()) {
            renamedCodeOwnersFile
                .value(renameTask.value.flatMap { it.renamedCodeOwnersFile })
                .disallowChanges()

            renamedCodeOwnersFileUntracked
                .value(project.layout.buildDirectory.file("codeowners/CODEOWNERS.renamed"))
                .disallowChanges()

            renameTask.value.configure task@{
                this@task.codeOwnersFile.value(this@CodeOwnersExtensionBaseInternal.codeOwnersFile)
                this@task.codeOwnersRenamer.value(this@CodeOwnersExtensionBaseInternal.codeOwnersRenamer)
                this@task.renamedCodeOwnersFile.value(this@CodeOwnersExtensionBaseInternal.renamedCodeOwnersFileUntracked)
            }
        }

        codeOwnersRenamer.value(renamer)
    }

}
