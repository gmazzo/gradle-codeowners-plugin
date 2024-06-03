@file:Suppress("UnstableApiUsage")

package io.github.gmazzo.codeowners

import io.github.gmazzo.codeowners.BuildConfig.ARG_CODEOWNERS_FILE
import io.github.gmazzo.codeowners.BuildConfig.ARG_CODEOWNERS_ROOT
import io.github.gmazzo.codeowners.BuildConfig.ARG_MAPPINGS_OUTPUT
import io.github.gmazzo.codeowners.BuildConfig.COMPILER_DEPENDENCY
import io.github.gmazzo.codeowners.BuildConfig.COMPILER_PLUGIN_ID
import io.github.gmazzo.codeowners.BuildConfig.CORE_DEPENDENCY
import io.github.gmazzo.codeowners.KotlinSupport.Companion.codeOwnersSourceSetName
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.codeOwners
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget

class CodeOwnersKotlinPlugin :
    CodeOwnersPlugin<CodeOwnersKotlinExtension>(CodeOwnersKotlinExtension::class.java),
    KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        super<CodeOwnersPlugin>.apply(target)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>) = true

    override fun getCompilerPluginId() = COMPILER_PLUGIN_ID

    override fun getPluginArtifact() = COMPILER_DEPENDENCY.split(':', limit = 3).let {
        SubpluginArtifact(groupId = it[0], artifactId = it[1], version = it[2])
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
        with(kotlinCompilation.project.the<CodeOwnersKotlinExtension>()) {
            kotlinCompilation.compileTaskProvider.configure {
                outputs.dir(kotlinCompilation.outputMappingsFile.map { it.asFile.parentFile }).optional()
            }

            rootDirectory
                .zip(codeOwnersFile.zip(kotlinCompilation.outputMappingsFile, ::Pair), ::Pair).map { (root, it) ->
                    val (file, mappings) = it

                    listOf(
                        SubpluginOption(ARG_CODEOWNERS_ROOT, root.asFile.absolutePath),
                        SubpluginOption(ARG_CODEOWNERS_FILE, file.asFile.absolutePath),
                        SubpluginOption(ARG_MAPPINGS_OUTPUT, mappings.asFile.absolutePath),
                    )
                }
        }

    private val KotlinCompilation<*>.outputMappingsFile: Provider<RegularFile>
        get() {
            val name = "${project.name}-$codeOwnersSourceSetName"
            return project.layout.buildDirectory.file("codeowners/mappings/$name/$name.codeowners")
        }

    override fun Project.configureExtension(extension: CodeOwnersKotlinExtension) =
        KotlinSupport(this).configureTargets target@{
            if (this !is KotlinMetadataTarget) {
                val targetExtension = objects.newInstance<CodeOwnersKotlinTargetExtension>()
                this@target.codeOwners = targetExtension

                targetExtension.enabled
                    .convention(true)
                    .finalizeValueOnRead()

                compilations.configureEach compilation@{
                    val sourceSet = extension.sourceSets.maybeCreate(codeOwnersSourceSetName)
                    this@compilation.codeOwners = sourceSet

                    sourceSet.enabled
                        .convention(targetExtension.enabled)
                        .finalizeValueOnRead()

                    sourceSet.mappings.from(outputMappingsFile)

                    addCodeDependency(sourceSet.enabled, defaultSourceSet.implementationConfigurationName)
                }

            } else {
                // metadata target still requires the `core` dependency
                compilations.configureEach {
                    addCodeDependency(provider { true }, defaultSourceSet.implementationConfigurationName)
                }
            }
        }

    private fun Project.addCodeDependency(
        enabled: Provider<Boolean>,
        configurationName: String,
    ) {
        dependencies.addProvider(
            configurationName,
            enabled.map { if (it) CORE_DEPENDENCY else files() }
        )
    }

}
