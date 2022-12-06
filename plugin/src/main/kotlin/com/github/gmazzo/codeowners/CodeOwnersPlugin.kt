package com.github.gmazzo.codeowners

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JvmEcosystemPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.*

class CodeOwnersPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        rootProject.apply<CodeOwnersRootPlugin>()
        apply<JvmEcosystemPlugin>()

        val extension = rootProject.extensions.getByType<CodeOwnersExtension>()

        val sourceSets = objects.domainObjectContainer(SourceDirectorySet::class) { name ->
            objects.sourceDirectorySet(name, "$name codeOwners sources")
        }
        sourceSets.configureEach { ss ->
            val generateTask = tasks.register<CodeOwnersTask>("generate${ss.name.capitalized()}CodeOwnersResources") {
                codeOwners.value(extension.codeOwners)
                codeOwnersRoot.value(extension.codeOwnersRoot)
                sourceDirectories.source(ss)
            }

            ss.destinationDirectory.value(layout.buildDirectory.dir("codeOwners/${ss.name}"))
            ss.compiledBy(generateTask, CodeOwnersTask::outputDirectory)
        }

        the<SourceSetContainer>().configureEach { ss ->

            val sources = sourceSets.maybeCreate(ss.name)
            sources.source(ss.java)
            ss.resources.source(sources)
            ss.extensions.add(SourceDirectorySet::class.java, "codeOwners", sources)

            plugins.withId("kotlin") {
                sources.source(ss.extensions.getByName<SourceDirectorySet>("kotlin"))
            }

            plugins.withId("groovy") {
                sources.source(ss.extensions.getByName<SourceDirectorySet>("groovy"))
            }

        }

    }

}
