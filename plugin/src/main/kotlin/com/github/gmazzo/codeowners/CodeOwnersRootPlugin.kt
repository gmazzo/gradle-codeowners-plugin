package com.github.gmazzo.codeowners

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

internal class CodeOwnersRootPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        check(target == rootProject) { "This plugin can only be applied at root project" }

        with(extensions.create<CodeOwnersExtension>("codeOwners")) {
            codeOwnersRoot
                .convention(layout.projectDirectory)
                .finalizeValueOnRead()

            codeOwnersFile
                .from(
                    codeOwnersRoot.file("CODEOWNERS"),
                    codeOwnersRoot.file(".github/CODEOWNERS"),
                    codeOwnersRoot.file(".gitlab/CODEOWNERS"),
                    codeOwnersRoot.file("docs/CODEOWNERS"),
                )
                .finalizeValueOnRead()

            codeOwners
                .convention(provider { codeOwnersFile.asFileTree.singleFile.useLines { CodeOwnersFile(it) } })
                .finalizeValueOnRead()
        }
    }

}
