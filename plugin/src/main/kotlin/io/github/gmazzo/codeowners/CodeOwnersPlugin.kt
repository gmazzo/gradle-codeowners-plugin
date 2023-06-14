@file:Suppress("UnstableApiUsage")

package io.github.gmazzo.codeowners

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Component
import com.android.build.api.variant.HasAndroidTest
import io.github.gmazzo.codeowners.plugin.BuildConfig
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.attributes.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*

class CodeOwnersPlugin : Plugin<Project> {

    private companion object {
        const val ARTIFACT_TYPE_CODEOWNERS = "codeowners"
        private const val ARTIFACT_TYPE_ANDROID_JAVA_RES = "android-java-res"
    }

    private val extensionName = Component::codeOwners.name

    override fun apply(target: Project): Unit = with(target) {
        rootProject.apply<CodeOwnersPlugin>()

        val extension = createExtension()
        val sourceSets = createSourceSets(extension)

        bindSourceSets(extension, sourceSets)
        setupAndroidSupport(extension, sourceSets)
        setupArtifactTransform(objects)
    }

    private fun Project.createExtension() = when (project) {
        rootProject -> extensions.create<CodeOwnersExtension>(extensionName).apply {
            rootDirectory
                .convention(layout.projectDirectory)
                .finalizeValueOnRead()

            val defaultLocations = files(
                "CODEOWNERS",
                ".github/CODEOWNERS",
                ".gitlab/CODEOWNERS",
                "docs/CODEOWNERS",
            )
            codeOwnersFile
                .convention(layout.file(provider { defaultLocations.asFileTree.singleOrNull() }))
                .finalizeValueOnRead()

            codeOwners
                .value(codeOwnersFile.asFile.map { file -> file.useLines { CodeOwnersFile(it) } }.orElse(provider {
                    error(
                        defaultLocations.joinToString(
                            prefix = "No CODEOWNERS file found! Default locations:\n",
                            separator = "\n"
                        ) {
                            "- ${it.toRelativeString(rootDir)}"
                        })
                }))
                .apply { disallowChanges() }
                .finalizeValueOnRead()

            includes.finalizeValueOnRead()
            excludes.add("hilt_aggregated_deps/**")
            excludes.finalizeValueOnRead()

            inspectDependencies
                .convention(CodeOwnersConfig.DependenciesMode.LOCAL_PROJECTS)
                .finalizeValueOnRead()

            addCoreDependency
                .convention(findProperty("codeowners.default.dependency")?.toString()?.toBoolean() != false)
                .finalizeValueOnRead()

            addCodeOwnershipAsResources
                .convention(true)
                .finalizeValueOnRead()
        }

        else -> rootProject.the<CodeOwnersExtension>().also { extensions.add(extensionName, it) }
    }

    private fun Project.createSourceSets(
        extension: CodeOwnersExtension,
    ) = objects.domainObjectContainer(CodeOwnersSourceSet::class) { name ->
        val prefix = when (name) {
            SourceSet.MAIN_SOURCE_SET_NAME -> ""
            else -> name.capitalize()
        }

        val generateTask = tasks.register<CodeOwnersTask>("generate${prefix}CodeOwnersResources") {
            codeOwners.value(extension.codeOwners)
            rootDirectory.value(extension.rootDirectory)
            outputDirectory.value(layout.buildDirectory.dir("codeOwners/resources/$name"))
            mappedCodeOwnersFileHeader.value("Generated CODEOWNERS file for module `${project.name}`, source set `$name`\n")
            mappedCodeOwnersFile.value(layout.buildDirectory.file("codeOwners/mappings/$name.$ARTIFACT_TYPE_CODEOWNERS"))
            rawMappedCodeOwnersFile.value(layout.buildDirectory.file("codeOwners/mappings/$name-raw.$ARTIFACT_TYPE_CODEOWNERS"))
        }

        objects.newInstance<CodeOwnersSourceSetImpl>(name, generateTask).apply {
            enabled.convention(true).finalizeValueOnRead()
            includes.addAll(extension.includes)
            includes.finalizeValueOnRead()
            excludes.addAll(extension.excludes)
            excludes.finalizeValueOnRead()
            inspectDependencies.convention(extension.inspectDependencies).finalizeValueOnRead()
        }
    }

    private fun Project.bindSourceSets(
        extension: CodeOwnersExtension,
        sourceSets: NamedDomainObjectContainer<CodeOwnersSourceSet>,
    ) = plugins.withId("java-base") {

        the<SourceSetContainer>().configureEach {
            val sourceSet = sourceSets.maybeCreate(name)

            sourceSet.generateTask {
                // will contain srcDirs of groovy, kotlin, etc. too
                sources.from(filteredSourceFiles(sourceSet, provider { allJava.srcDirs }))
                addDependencies(objects, sourceSet, configurations[runtimeClasspathConfigurationName])
            }
            addCodeDependency(extension, sourceSet, implementationConfigurationName)

            if (name == SourceSet.MAIN_SOURCE_SET_NAME) {
                addOutgoingVariant(sourceSet, configurations[runtimeClasspathConfigurationName])
            }

            extensions.add(CodeOwnersSourceSet::class.java, extensionName, sourceSet)
            tasks.named<AbstractCopyTask>(processResourcesTaskName)
                .addResources(extension, sourceSet)
        }
    }

    private fun Project.setupAndroidSupport(
        extension: CodeOwnersExtension,
        sourceSets: NamedDomainObjectContainer<CodeOwnersSourceSet>,
    ) = plugins.withId("com.android.base") {
        val androidComponents: AndroidComponentsExtension<*, *, *> by extensions

        fun bind(
            component: Component,
            defaultsTo: CodeOwnersSourceSet? = null,
        ): CodeOwnersSourceSet {
            val sourceSet = sourceSets.maybeCreate(component.name).also(component::codeOwners.setter)
            sourceSet.generateTask {
                sources.from(filteredSourceFiles(sourceSet, component.sources.java?.all, component.sources.kotlin?.all))
                addDependencies(objects, sourceSet, component.runtimeConfiguration)
            }
            addCodeDependency(extension, sourceSet, component.compileConfiguration.name)
            addCodeDependency(extension, sourceSet, component.runtimeConfiguration.name)

            // TODO there is no `variant.sources.resources.addGeneratedSourceDirectory` DSL for this?
            afterEvaluate {
                tasks.named<AbstractCopyTask>("process${component.name.capitalize()}JavaRes")
                    .addResources(extension, sourceSet)
            }

            if (defaultsTo != null) {
                sourceSet.enabled.convention(defaultsTo.enabled)
            }
            return sourceSet
        }

        androidComponents.onVariants(androidComponents.selector().all()) { variant ->
            variant.packaging.resources.merges.add("**/*.codeowners")

            val sources = bind(variant)

            variant.unitTest?.let { bind(component = it, defaultsTo = sources) }
            (variant as? HasAndroidTest)?.androidTest?.let { bind(component = it, defaultsTo = sources) }

            addOutgoingVariant(sources, variant.runtimeConfiguration).configure {
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
    }

    private fun Project.filteredSourceFiles(
        sourceSet: CodeOwnersSourceSet,
        vararg files: Provider<*>?,
    ) = files(files).asFileTree.matching {
        includes += sourceSet.includes.get()
        excludes += sourceSet.excludes.get()
    }

    private fun CodeOwnersTask.addDependencies(
        objects: ObjectFactory,
        sourceSet: CodeOwnersSourceSet,
        configuration: Configuration,
    ) {
        val mode = sourceSet.inspectDependencies.get()

        if (mode != CodeOwnersConfig.DependenciesMode.NONE) {
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

                if (mode == CodeOwnersConfig.DependenciesMode.LOCAL_PROJECTS) {
                    componentFilter { it is ProjectComponentIdentifier }
                }
            }.files)
        }
    }

    private fun TaskProvider<out AbstractCopyTask>.addResources(
        extension: CodeOwnersExtension,
        sources: CodeOwnersSourceSet
    ) = configure {
        from(extension.addCodeOwnershipAsResources.and(sources.enabled).map {
            if (it) sources.generateTask.map(CodeOwnersTask::outputDirectory) else emptyList<Any>()
        })
    }

    private fun Project.addCodeDependency(
        extension: CodeOwnersExtension,
        sourceSet: CodeOwnersSourceSet,
        configurationName: String,
    ) {
        dependencies.constraints.add(configurationName, BuildConfig.CORE_DEPENDENCY)

        afterEvaluate {
            dependencies.add(
                configurationName,
                extension.addCoreDependency.and(extension.addCodeOwnershipAsResources, sourceSet.enabled)
                    .map { if (it) BuildConfig.CORE_DEPENDENCY else files() })
        }
    }

    private fun Project.addOutgoingVariant(
        sourceSet: CodeOwnersSourceSet,
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
