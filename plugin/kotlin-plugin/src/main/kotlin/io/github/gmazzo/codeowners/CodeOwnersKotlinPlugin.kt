@file:Suppress("UnstableApiUsage")

package io.github.gmazzo.codeowners

import io.github.gmazzo.codeowners.BuildConfig.ARG_CODEOWNERS_FILE
import io.github.gmazzo.codeowners.BuildConfig.ARG_CODEOWNERS_ROOT
import io.github.gmazzo.codeowners.BuildConfig.COMPILER_DEPENDENCY
import io.github.gmazzo.codeowners.BuildConfig.COMPILER_PLUGIN_ID
import io.github.gmazzo.codeowners.BuildConfig.CORE_DEPENDENCY
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.codeOwners
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainer
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class CodeOwnersKotlinPlugin :
    CodeOwnersBasePlugin<CodeOwnersExtension>(CodeOwnersExtension::class.java),
    KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        super<CodeOwnersBasePlugin>.apply(target)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>) = true

    override fun getCompilerPluginId() = COMPILER_PLUGIN_ID

    override fun getPluginArtifact() = COMPILER_DEPENDENCY.split(':', limit = 3).let {
        SubpluginArtifact(groupId = it[0], artifactId = it[1], version = it[2])
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
        with(kotlinCompilation.project.the<CodeOwnersExtension>()) {
            rootDirectory.asFile.zip(codeOwnersFile.asFile, ::Pair).map { (root, file) ->
                listOf(
                    SubpluginOption(ARG_CODEOWNERS_ROOT, root.absolutePath),
                    SubpluginOption(ARG_CODEOWNERS_FILE, file.absolutePath)
                )
            }
        }

    override fun Project.configure(
        extension: CodeOwnersExtension,
        parent: CodeOwnersExtension?,
        defaultLocations: FileCollection
    ) {
        extension.enableRuntimeSupport
            .valueIfNotNull(parent?.enableRuntimeSupport)
            .convention(true)
            .finalizeValueOnRead()

        configureKotlinExtension(extension)
    }

    private fun Project.configureKotlinExtension(
        extension: CodeOwnersExtension,
    ) = plugins.withType<KotlinBasePlugin> {
        val reportTask = tasks.register<CodeOwnersReportTask>("codeOwnersReport") {
            group = "CodeOwners"
            description = "Generate CODEOWNERS report for all targets"

            rootDirectory.set(extension.rootDirectory)
            codeOwnersFile.set(extension.codeOwnersFile)
            codeOwnersReportHeader.set("CodeOwners of module ${project.name}")
            codeOwnersReportFile.set(layout.buildDirectory.file("reports/codeowners/${pathAsFileName}.properties"))
        }

        fun KotlinTarget.configure(single: Boolean) {
            val targetName = this@configure.name

            val targetReportTask =
                if (single) null
                else tasks.register<CodeOwnersReportTask>("${targetName}CodeOwnersReport") {
                    group = "CodeOwners"
                    description = "Generate CODEOWNERS report for '$targetName' target"

                    rootDirectory.set(extension.rootDirectory)
                    codeOwnersFile.set(extension.codeOwnersFile)
                    codeOwnersReportHeader.set("CodeOwners of $targetName of module ${project.path}")
                    codeOwnersReportFile.set(layout.buildDirectory.file("reports/codeowners/${project.pathAsFileName}/$targetName.properties"))
                }

            val targetExtension = objects.newInstance<CodeOwnersCompilationExtension>()
                .also(::codeOwners.setter)

            targetExtension.enabled
                .convention(extension.enableRuntimeSupport)
                .finalizeValueOnRead()

            compilations.configureEach {
                val compilationExtension = objects.newInstance<CodeOwnersCompilationExtension>()
                    .also(::codeOwners.setter)

                compilationExtension.enabled
                    .convention(targetExtension.enabled)
                    .finalizeValueOnRead()

                addCodeDependency(compilationExtension, defaultSourceSet.implementationConfigurationName)

                listOfNotNull(reportTask, targetReportTask).forEach {
                    it.configure {
                        sources.from(allKotlinSourceSets.map { it.kotlin })
                    }
                }
            }
        }

        when (val kotlin = extensions.getByName("kotlin")) {
            is KotlinSingleTargetExtension<*> -> kotlin.target.configure(single = true)
            is KotlinTargetsContainer -> kotlin.targets.configureEach { configure(single = false) }
        }
    }

    private fun Project.addCodeDependency(
        target: CodeOwnersCompilationExtension,
        configurationName: String,
    ) {
        dependencies.addProvider(
            configurationName,
            target.enabled.map { if (it) CORE_DEPENDENCY else files() })
    }

    private val Project.pathAsFileName
        get() = path.removePrefix(":").replace(':', '-')

}
