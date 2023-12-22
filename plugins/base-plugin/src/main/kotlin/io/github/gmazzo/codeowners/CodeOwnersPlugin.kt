package io.github.gmazzo.codeowners

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainer

open class CodeOwnersPlugin<Extension : CodeOwnersExtension>(
    @Suppress("UNCHECKED_CAST")
    private val extensionClass: Class<out Extension> = CodeOwnersExtension::class.java as Class<Extension>,
) : Plugin<Project> {

    companion object {
        const val TASK_GROUP = "CodeOwners"
    }

    open fun Project.configure(extension: Extension, parent: Extension?, defaultLocations: FileCollection) {}

    override fun apply(target: Project): Unit = with(target) {
        val parentExtension = generateSequence(parent) { it.parent }
            .mapNotNull { it.extensions.findByType(extensionClass) }
            .firstOrNull()

        val extension: Extension = extensions.create("codeOwners", extensionClass).apply {

            rootDirectory
                .valueIfNotNull(parentExtension?.rootDirectory)
                .convention(layout.projectDirectory)
                .finalizeValueOnRead()

            val defaultLocations = files(
                "CODEOWNERS",
                ".github/CODEOWNERS",
                ".gitlab/CODEOWNERS",
                "docs/CODEOWNERS",
            )

            val defaultFile = lazy {
                defaultLocations.asFileTree.singleOrNull() ?: error(defaultLocations.joinToString(
                    prefix = "No CODEOWNERS file found! Default locations:\n",
                    separator = "\n"
                ) { "- ${it.toRelativeString(rootDir)}" })
            }

            codeOwnersFile
                .valueIfNotNull(parentExtension?.codeOwnersFile)
                .convention(layout.file(provider(defaultFile::value)))
                .finalizeValueOnRead()

            configure(this, parentExtension, defaultLocations)
        }

        afterEvaluate {
            val reportAllTask by lazy {
                tasks.register<CodeOwnersReportTask>("codeOwnersReport") {
                    group = TASK_GROUP
                    description = "Generates CODEOWNERS report for all classes of this project"

                    rootDirectory.set(extension.rootDirectory)
                    codeOwnersFile.set(extension.codeOwnersFile)
                    codeOwnersReportHeader.set("CodeOwners of module '${project.path}'\n")
                    codeOwnersReportFile.set(layout.buildDirectory.file("reports/${project.name}.codeowners"))
                }
            }

            when {
                extensions.findByName("kotlin") != null -> configureReportTasksByKotlinTargets(extension, reportAllTask)
                plugins.hasPlugin("java-base") -> configureReportTasksByJavaSourceSets(extension, reportAllTask)
            }
        }
    }

    protected fun <Type, PropertyOfType : Property<Type>> PropertyOfType.valueIfNotNull(
        provider: Provider<Type>?,
    ): PropertyOfType = apply {
        if (provider != null) {
            value(provider)
        }
    }

    private fun Project.configureReportTasksByKotlinTargets(
        extension: Extension,
        reportAllTask: TaskProvider<CodeOwnersReportTask>,
    ) {
        fun KotlinTarget.configureReports() {
            val reportTask = createReportTask(name, "target", extension)

            compilations.configureEach {
                sequenceOf(reportAllTask, reportTask).forEach {
                    it.configure {
                        sources.from(allKotlinSourceSets.map(KotlinSourceSet::kotlin))
                    }
                }
            }
        }

        when (val kotlin = extensions.findByName("kotlin")) {
            is KotlinSingleTargetExtension<*> -> kotlin.target.configureReports()
            is KotlinTargetsContainer -> kotlin.targets.configureEach(KotlinTarget::configureReports)
        }
    }

    private fun Project.configureReportTasksByJavaSourceSets(
        extension: Extension,
        reportAllTask: TaskProvider<CodeOwnersReportTask>,
    ) {
        the<SourceSetContainer>().configureEach {
            val reportTask = createReportTask(name, "source set", extension)

            sequenceOf(reportAllTask, reportTask).forEach {
                it.configure {
                    sources.from(allJava.sourceDirectories)
                }
            }
        }
    }

    private fun Project.createReportTask(
        targetName: String,
        targetType: String,
        extension: Extension,
    ) = tasks.register<CodeOwnersReportTask>("${targetName}CodeOwnersReport") {
        group = TASK_GROUP
        description = "Generates CODEOWNERS report for '$targetName' $targetType"

        rootDirectory.set(extension.rootDirectory)
        codeOwnersFile.set(extension.codeOwnersFile)
        codeOwnersReportHeader.set("CodeOwners of '$targetName' $targetType of module '${project.path}'\n")
        codeOwnersReportFile.set(layout.buildDirectory.file("reports/${project.name}-$targetName.codeowners"))
    }

    private val Project.pathAsFileName
        get() = path.removePrefix(":").replace(':', '-')

}
