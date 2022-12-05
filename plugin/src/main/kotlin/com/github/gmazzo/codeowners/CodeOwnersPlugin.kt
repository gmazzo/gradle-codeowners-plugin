package com.github.gmazzo.codeowners

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JvmEcosystemPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the

class CodeOwnersPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        rootProject.apply<CodeOwnersRootPlugin>()
        apply<JvmEcosystemPlugin>()

        val extension = rootProject.extensions.getByType<CodeOwnersExtension>()

        the<SourceSetContainer>().configureEach { ss ->

            val generateTask = tasks.register<CodeOwnersTask>("generate${ss.name.capitalized()}PackageInfo") {
                codeOwners.value(extension.codeOwners)
                codeOwnersRoot.value(extension.codeOwnersRoot)
                sourceDirectories.from(ss.java)
                outputDirectory.value(layout.buildDirectory.dir("codeOwners/${ss.name}"))
            }

            ss.java.srcDir(generateTask)

            plugins.withId("kotlin") {
                generateTask.configure {
                    it.sourceDirectories.from(ss.extensions.getByName("kotlin"))
                }
            }

            plugins.withId("groovy") {
                generateTask.configure {
                    it.sourceDirectories.from(ss.extensions.getByName("groovy"))
                }
            }

        }

    }

}
