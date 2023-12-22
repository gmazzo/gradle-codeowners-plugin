@file:Suppress("UnstableApiUsage")

package io.github.gmazzo.codeowners

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Component
import com.android.build.api.variant.HasAndroidTest
import com.android.build.api.variant.HasUnitTest
import io.github.gmazzo.codeowners.matcher.CodeOwnersFile
import org.gradle.api.NamedDomainObjectContainer
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
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.codeOwners
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerTransform
import org.gradle.kotlin.dsl.the
import org.gradle.util.GradleVersion

class CodeOwnersJVMPlugin : CodeOwnersPlugin<CodeOwnersJVMExtension>(CodeOwnersJVMExtension::class.java) {

    private companion object {
        const val ARTIFACT_TYPE_CODEOWNERS = "codeowners"
        private const val ARTIFACT_TYPE_ANDROID_JAVA_RES = "android-java-res"
    }

    override fun Project.configure(
        extension: CodeOwnersJVMExtension,
        parent: CodeOwnersJVMExtension?,
        defaultLocations: FileCollection
    ) {
        GradleVersion.version("7.5").let { required ->
            check(GradleVersion.current() >= required) {
                "`io.github.gmazzo.codeowners.jvm` plugin requires $required or higher"
            }
        }

        configureExtension(extension, parent)
        val codeOwners = extension.codeOwnersFile.asFile.map { file -> file.useLines { CodeOwnersFile(it) } }
        val sourceSets = createSourceSets(extension, codeOwners)

        bindSourceSets(extension, sourceSets)
        setupAndroidSupport(extension, sourceSets)
        setupArtifactTransform(objects)
    }

    private fun Project.configureExtension(extension: CodeOwnersJVMExtension, parent: CodeOwnersJVMExtension?) =
        with(extension) {
            includes.finalizeValueOnRead()
            excludes.add("hilt_aggregated_deps/**")
            excludes.finalizeValueOnRead()

            inspectDependencies
                .valueIfNotNull(parent?.inspectDependencies)
                .convention(CodeOwnersConfig.DependenciesMode.LOCAL_PROJECTS)
                .finalizeValueOnRead()

            addCodeOwnershipAsResources
                .valueIfNotNull(parent?.addCodeOwnershipAsResources)
                .convention(true)
                .finalizeValueOnRead()
        }

    private fun Project.createSourceSets(
        extension: CodeOwnersJVMExtension,
        codeOwnersFile: Provider<CodeOwnersFile>,
    ) = objects.domainObjectContainer(CodeOwnersSourceSet::class) { name ->
        @Suppress("DEPRECATION")
        val prefix = when (name) {
            SourceSet.MAIN_SOURCE_SET_NAME -> ""
            else -> name.capitalize()
        }

        val generateTask = tasks.register<CodeOwnersTask>("generate${prefix}CodeOwnersResources") {
            group = TASK_GROUP
            description = "Process CODEOWNERS entries for source set '$name'"

            codeOwners.set(codeOwnersFile)
            rootDirectory.set(extension.rootDirectory)
            outputDirectory.set(layout.buildDirectory.dir("codeOwners/resources/$name"))
            mappedCodeOwnersFileHeader.set("Generated CODEOWNERS file for module '${project.path}', source set '$name'\n")
            mappedCodeOwnersFile.set(layout.buildDirectory.file("codeOwners/mappings/$name-simplified.codeowners"))
            rawMappedCodeOwnersFile.set(layout.buildDirectory.file("codeOwners/mappings/$name-raw.codeowners"))
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
        extension: CodeOwnersJVMExtension,
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

            extensions.add(CodeOwnersSourceSet::class.java, Component::codeOwners.name, sourceSet)
            tasks.named<AbstractCopyTask>(processResourcesTaskName).addResources(extension, sourceSet)
        }
    }

    private fun Project.setupAndroidSupport(
        extension: CodeOwnersJVMExtension,
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
                @Suppress("DEPRECATION")
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

            (variant as? HasUnitTest)?.unitTest?.let { bind(component = it, defaultsTo = sources) }
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
        extension: CodeOwnersJVMExtension,
        sources: CodeOwnersSourceSet
    ) = configure {
        from(extension.addCodeOwnershipAsResources.and(sources.enabled).map {
            if (it) sources.generateTask.map(CodeOwnersTask::outputDirectory) else emptyList<Any>()
        })
    }

    private fun Project.addCodeDependency(
        extension: CodeOwnersJVMExtension,
        sourceSet: CodeOwnersSourceSet,
        configurationName: String,
    ) {
        dependencies.add(
            configurationName,
            extension.addCodeOwnershipAsResources.and(sourceSet.enabled)
                .map { if (it) BuildConfig.CORE_DEPENDENCY else files() })
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