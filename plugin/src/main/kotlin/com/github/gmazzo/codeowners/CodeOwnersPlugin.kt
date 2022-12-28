package com.github.gmazzo.codeowners

import com.android.build.api.variant.AndroidComponentsExtension
import com.github.gmazzo.codeowners.plugin.BuildConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JvmEcosystemPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.*

class CodeOwnersPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        apply<JvmEcosystemPlugin>()

        val extension: CodeOwnersExtension = when (project) {
            rootProject -> createExtension()
            else -> {
                check(rootProject.plugins.hasPlugin(CodeOwnersPlugin::class)) { "The codeowners plugin must also be applied at root project" }
                rootProject.the()
            }
        }

        val sourceSets = objects.domainObjectContainer(CodeOwnersSourceSet::class) { name ->
            val ss = objects.sourceDirectorySet(name, "$name codeOwners sources")

            val prefix = when (name) {
                SourceSet.MAIN_SOURCE_SET_NAME -> ""
                else -> name.capitalized()
            }

            val generateTask = tasks.register<CodeOwnersTask>("generate${prefix}CodeOwnersResources") {
                codeOwners.value(extension.codeOwners)
                rootDirectory.value(extension.rootDirectory)
                sourceFiles.from(ss)
            }

            ss.destinationDirectory.value(layout.buildDirectory.dir("codeOwners/${ss.name}"))
            ss.compiledBy(generateTask, CodeOwnersTask::outputDirectory)

            CodeOwnersSourceSet(ss, generateTask)
        }

        the<SourceSetContainer>().configureEach { ss ->
            val sources = sourceSets.maybeCreate(ss.name)
            sources.source(ss.java)
            ss.resources.srcDir(sources.generateTask)
            ss.extensions.add(SourceDirectorySet::class.java, "codeOwners", sources.sources)

            plugins.withId("kotlin") {
                val kotlin: SourceDirectorySet by ss.extensions

                sources.source(kotlin)
            }

            plugins.withId("groovy") {
                val groovy: SourceDirectorySet by ss.extensions

                sources.source(groovy)
            }

        }

        plugins.withId("java") {
            dependencies {
                "implementation"(BuildConfig.CORE_DEPENDENCY)
            }
        }

        plugins.withId("com.android.base") {
            val androidComponents: AndroidComponentsExtension<*, *, *> by extensions

            androidComponents.onVariants { variant ->
                val sources = sourceSets.maybeCreate(variant.name)

                sources.srcDir(listOfNotNull(variant.sources.java?.all, variant.sources.kotlin?.all))
                variant.sources.getByName("resources")
                    .addGeneratedSourceDirectory(sources.generateTask, CodeOwnersTask::outputDirectory)
            }

            dependencies {
                "implementation"(BuildConfig.CORE_DEPENDENCY)
            }
        }

    }

    private fun Project.createExtension() = extensions.create<CodeOwnersExtension>("codeOwners").apply {
        rootDirectory
            .convention(layout.projectDirectory)
            .finalizeValueOnRead()

        codeOwnersFile
            .from(
                rootDirectory.file("CODEOWNERS"),
                rootDirectory.file(".github/CODEOWNERS"),
                rootDirectory.file(".gitlab/CODEOWNERS"),
                rootDirectory.file("docs/CODEOWNERS"),
            )
            .finalizeValueOnRead()

        codeOwners
            .convention(provider { codeOwnersFile.asFileTree.singleFile.useLines { CodeOwnersFile(it) } })
            .finalizeValueOnRead()
    }

    private class CodeOwnersSourceSet(
        val sources: SourceDirectorySet,
        val generateTask: TaskProvider<CodeOwnersTask>,
    ) : SourceDirectorySet by sources

}
