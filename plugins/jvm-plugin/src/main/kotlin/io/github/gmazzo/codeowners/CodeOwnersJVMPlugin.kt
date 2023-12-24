@file:Suppress("UnstableApiUsage")

package io.github.gmazzo.codeowners

import com.android.build.api.variant.Component
import com.android.build.api.variant.HasAndroidTest
import com.android.build.api.variant.HasUnitTest
import io.github.gmazzo.codeowners.CodeOwnersInspectDependencies.DependenciesMode
import io.github.gmazzo.codeowners.matcher.CodeOwnersFile
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.HasAttributes
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.codeOwners
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.generateTask
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerTransform
import org.gradle.kotlin.dsl.the

class CodeOwnersJVMPlugin : CodeOwnersPlugin<CodeOwnersJVMExtension>(CodeOwnersJVMExtension::class.java) {

    private companion object {
        const val ARTIFACT_TYPE_CODEOWNERS = "codeowners"
        private const val ARTIFACT_TYPE_ANDROID_JAVA_RES = "android-java-res"
    }

    override fun Project.configureExtension(extension: CodeOwnersJVMExtension) {
        val codeOwners = extension.codeOwnersFile.asFile.map { file -> file.useLines { CodeOwnersFile(it) } }

        extension.inspectDependencies
            .convention(DependenciesMode.LOCAL_PROJECTS)
            .finalizeValueOnRead()

        extension.addCodeOwnershipAsResources
            .convention(true)
            .finalizeValueOnRead()

        configureSourceSets(extension, codeOwners)
        setupArtifactTransform(objects)
    }

    private fun Project.configureSourceSets(
        extension: CodeOwnersJVMExtension,
        codeOwnersFile: Provider<CodeOwnersFile>,
    ) = extension.sourceSets.configureEach ss@{

        enabled
            .convention(true)
            .finalizeValueOnRead()

        inspectDependencies
            .convention(extension.inspectDependencies)
            .finalizeValueOnRead()

        @Suppress("DEPRECATION")
        val prefix = when (name) {
            SourceSet.MAIN_SOURCE_SET_NAME -> ""
            else -> name.capitalize()
        }

        generateTask = tasks.register<CodeOwnersTask>("generate${prefix}CodeOwnersResources") {
            group = TASK_GROUP
            description = "Process CODEOWNERS entries for source set '${this@ss.name}'"

            sources.from(this@ss.sources)
            codeOwners.set(codeOwnersFile)
            rootDirectory.set(extension.rootDirectory)
            outputDirectory.set(layout.buildDirectory.dir("codeOwners/resources/${this@ss.name}"))
            rawMappedCodeOwnersFile.set(layout.buildDirectory.file("codeOwners/mappings/${this@ss.name}-raw.codeowners"))
            simplifiedMappedCodeOwnersFile.set(layout.buildDirectory.file("codeOwners/mappings/${this@ss.name}-simplified.codeowners"))
        }
    }

    override fun Project.configureBySourceSet(extension: CodeOwnersJVMExtension) {
        the<SourceSetContainer>().configureEach ss@{
            val sourceSet = extension.sourceSets.maybeCreate(this@ss.name)

            sourceSet.generateTask {
                addDependencies(objects, sourceSet, configurations[runtimeClasspathConfigurationName])
            }
            addCodeDependency(extension, sourceSet, implementationConfigurationName)

            if (name == SourceSet.MAIN_SOURCE_SET_NAME) {
                addOutgoingVariant(sourceSet, configurations[runtimeClasspathConfigurationName])
            }

            this@ss.extensions.add(CodeOwnersJVMSourceSet::class.java, Component::codeOwners.name, sourceSet)
            tasks.named<AbstractCopyTask>(processResourcesTaskName).addResources(extension, sourceSet)
        }
    }

    override fun Project.configureByAndroidVariants(extension: CodeOwnersJVMExtension) {
        plugins.withId("com.android.base") {

        fun bind(
            component: Component,
            defaultsTo: CodeOwnersJVMSourceSet? = null,
        ): CodeOwnersJVMSourceSet {
            val sourceSet = extension.sourceSets.maybeCreate(component.name)
            component.codeOwners = sourceSet
            sourceSet.generateTask {
                addDependencies(objects, sourceSet, component.runtimeConfiguration)
            }
            addCodeDependency(extension, sourceSet, component.compileConfiguration.name)
            addCodeDependency(extension, sourceSet, component.runtimeConfiguration.name)

            // TODO there is no `variant.sources.resources.addGeneratedSourceDirectory` DSL for this?
            afterEvaluate {
                @Suppress("DEPRECATION")
                tasks.named<AbstractCopyTask>("process${component.name.capitalize()}JavaRes")
                    .addResources(extension, sourceSet)
            }

            if (defaultsTo != null) {
                sourceSet.enabled.convention(defaultsTo.enabled)
            }
            return sourceSet
        }

        AndroidSupport(project).configureVariants {
            val variant = this@configureVariants
            variant.packaging.resources.merges.add("**/*.codeowners")

            val sourceSet = bind(variant)

            (variant as? HasUnitTest)?.unitTest?.let { bind(component = it, defaultsTo = sourceSet) }
            (variant as? HasAndroidTest)?.androidTest?.let { bind(component = it, defaultsTo = sourceSet) }

            addOutgoingVariant(sourceSet, variant.runtimeConfiguration).configure {
                val attrs = variant.runtimeConfiguration.attributes

                // copies variant attributes
                attributes {
                    attrs.keySet().forEach { key ->
                        if (key.name.startsWith("com.android")) {
                            @Suppress("UNCHECKED_CAST")
                            attribute(key as Attribute<Any>, attrs.getAttribute(key)!!)
                        }
                    }
                }
            }
        }
    }}

    private fun CodeOwnersTask.addDependencies(
        objects: ObjectFactory,
        sourceSet: CodeOwnersJVMSourceSet,
        configuration: Configuration,
    ) {
        val mode = sourceSet.inspectDependencies.get()

        if (mode != DependenciesMode.NONE) {
            transitiveCodeOwners.from(configuration.incoming.artifactView {
                /**
                 * [configuration] is `runtimeClasspath`, and we want to allow the `codeowners` outgoing variant to be picked
                 * otherwise, to avoid an unnecessary [CodeOwnersTransform] to be performed on its `.jar`
                 */
                withVariantReselection()

                attributes {
                    copyNonStandardAttributes(configuration)
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(ARTIFACT_TYPE_CODEOWNERS))
                    attribute(ARTIFACT_TYPE_ATTRIBUTE, ARTIFACT_TYPE_CODEOWNERS)
                }

                if (mode == DependenciesMode.LOCAL_PROJECTS) {
                    componentFilter { it is ProjectComponentIdentifier }
                }
            }.files)
        }
    }

    private fun TaskProvider<out AbstractCopyTask>.addResources(
        extension: CodeOwnersJVMExtension,
        sources: CodeOwnersJVMSourceSet
    ) = configure {
        from(extension.addCodeOwnershipAsResources.and(sources.enabled).map {
            if (it) sources.generateTask.map(CodeOwnersTask::outputDirectory) else emptyList<Any>()
        })
    }

    private fun Project.addCodeDependency(
        extension: CodeOwnersJVMExtension,
        sourceSet: CodeOwnersJVMSourceSet,
        configurationName: String,
    ) {
        dependencies.add(
            configurationName,
            extension.addCodeOwnershipAsResources.and(sourceSet.enabled)
                .map { if (it) BuildConfig.CORE_DEPENDENCY else files() })
    }

    private fun Project.addOutgoingVariant(
        sourceSet: CodeOwnersJVMSourceSet,
        base: Configuration,
    ) = configurations.register("${sourceSet.name}CodeOwnersElements") {
        isCanBeResolved = false
        isCanBeConsumed = true
        description = "CODEOWNERS information for the sourceSet ${sourceSet.name}"
        attributes {
            copyNonStandardAttributes(base)
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(ARTIFACT_TYPE_CODEOWNERS))
        }
        outgoing {
            base.outgoing.capabilities.forEach(::capability)
            artifacts(sourceSet.enabled.map { enabled ->
                if (enabled) listOf(sourceSet.generateTask.flatMap { it.rawMappedCodeOwnersFile })
                else emptyList()
            }) { type = ARTIFACT_TYPE_CODEOWNERS }
        }
    }

    private fun Project.setupArtifactTransform(
        objects: ObjectFactory,
    ) = dependencies {
        registerTransform(CodeOwnersTransform::class) {
            from.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            from.attribute(ARTIFACT_TYPE_ATTRIBUTE, JAR_TYPE)
            to.attribute(Usage.USAGE_ATTRIBUTE, objects.named(ARTIFACT_TYPE_CODEOWNERS))
            to.attribute(ARTIFACT_TYPE_ATTRIBUTE, ARTIFACT_TYPE_CODEOWNERS)
        }
        registerTransform(CodeOwnersTransform::class) {
            from.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            from.attribute(ARTIFACT_TYPE_ATTRIBUTE, ARTIFACT_TYPE_ANDROID_JAVA_RES)
            to.attribute(Usage.USAGE_ATTRIBUTE, objects.named(ARTIFACT_TYPE_CODEOWNERS))
            to.attribute(ARTIFACT_TYPE_ATTRIBUTE, ARTIFACT_TYPE_CODEOWNERS)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <Type : HasConfigurableAttributes<Type>> Type.copyNonStandardAttributes(
        from: HasAttributes,
    ) = attributes {
        from.attributes.keySet().forEach {
            when (it.name) {
                Usage.USAGE_ATTRIBUTE.name,
                Category.CATEGORY_ATTRIBUTE.name,
                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE.name,
                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE.name,
                "org.jetbrains.kotlin.platform.type" -> Unit // do nothing
                else -> attribute(it as Attribute<Any>, from.attributes.getAttribute(it)!!)
            }
        }
    }

    private fun Provider<Boolean>.and(vararg others: Provider<Boolean>) =
        sequenceOf(this, *others).reduce { acc, it -> acc.zip(it, Boolean::and) }

}
