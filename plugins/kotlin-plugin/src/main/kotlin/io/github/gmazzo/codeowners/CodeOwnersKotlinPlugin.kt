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
            rootDirectory.asFile.zip(codeOwnersFile.asFile, ::Pair).map { (root, file) ->
                listOf(
                    SubpluginOption(ARG_CODEOWNERS_ROOT, root.absolutePath),
                    SubpluginOption(ARG_CODEOWNERS_FILE, file.absolutePath)
                )
            }
        }

    override fun Project.configure(
        extension: CodeOwnersKotlinExtension,
        parent: CodeOwnersKotlinExtension?,
        defaultLocations: FileCollection
    ) {
        extension.enableRuntimeSupport
            .valueIfNotNull(parent?.enableRuntimeSupport)
            .convention(true)
            .finalizeValueOnRead()

        configureKotlinExtension(extension)
    }

    private fun Project.configureKotlinExtension(
        extension: CodeOwnersKotlinExtension,
    ) = plugins.withType<KotlinBasePlugin> {
        fun KotlinTarget.configure() {
            val targetExtension = objects.newInstance<CodeOwnersKotlinCompilationExtension>()
                .also(::codeOwners.setter)

            targetExtension.enabled
                .convention(extension.enableRuntimeSupport)
                .finalizeValueOnRead()

            compilations.configureEach {
                val compilationExtension = objects.newInstance<CodeOwnersKotlinCompilationExtension>()
                    .also(::codeOwners.setter)

                compilationExtension.enabled
                    .convention(targetExtension.enabled)
                    .finalizeValueOnRead()

                addCodeDependency(compilationExtension, defaultSourceSet.implementationConfigurationName)
            }
        }

        when (val kotlin = extensions.getByName("kotlin")) {
            is KotlinSingleTargetExtension<*> -> kotlin.target.configure()
            is KotlinTargetsContainer -> kotlin.targets.configureEach { configure() }
        }
    }

    private fun Project.addCodeDependency(
        target: CodeOwnersKotlinCompilationExtension,
        configurationName: String,
    ) {
        dependencies.addProvider(
            configurationName,
            target.enabled.map { if (it) CORE_DEPENDENCY else files() })
    }

}
