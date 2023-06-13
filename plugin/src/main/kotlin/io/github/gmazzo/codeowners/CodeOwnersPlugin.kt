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
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*

class CodeOwnersPlugin : Plugin<Project> {

    private companion object {
        const val ARTIFACT_TYPE_CODEOWNERS = "codeowners"
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

            inspectDependencies
                .convention(InspectDependencies.Mode.LOCAL_PROJECTS)
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
            mappedCodeOwnersFile.value(layout.buildDirectory.file("codeOwners/mappings/$name.CODEOWNERS"))
        }

        objects.newInstance<CodeOwnersSourceSetImpl>(name, generateTask).apply {
            enabled.convention(true).finalizeValueOnRead()
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
                sources.from(provider { allJava.srcDirs }) // will contain srcDirs of groovy, kotlin, etc. too
                addDependencies(objects, sourceSet, configurations[runtimeClasspathConfigurationName])
            }
            addCodeDependency(extension, sourceSet, implementationConfigurationName)

            if (name == SourceSet.MAIN_SOURCE_SET_NAME) {
                addOutgoingVariant(sourceSet, configurations[runtimeClasspathConfigurationName].attributes)
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
                sources.from(component.sources.java?.all, component.sources.kotlin?.all)
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

            addOutgoingVariant(sources, variant.runtimeConfiguration.attributes)
        }
    }

    private fun CodeOwnersTask.addDependencies(
        objects: ObjectFactory,
        sourceSet: CodeOwnersSourceSet,
        configuration: Configuration,
    ) {
        val mode = sourceSet.inspectDependencies.get()
        if (mode == InspectDependencies.Mode.NONE) return

        transitiveCodeOwners.from(configuration.incoming.artifactView {
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                attribute(ARTIFACT_TYPE_ATTRIBUTE, ARTIFACT_TYPE_CODEOWNERS)
            }

            if (mode == InspectDependencies.Mode.LOCAL_PROJECTS) {
                componentFilter { it is ProjectComponentIdentifier }
            }
        }.files)
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

        dependencies.add(
            configurationName,
            extension.addCoreDependency.and(extension.addCodeOwnershipAsResources, sourceSet.enabled)
                .map { if (it) BuildConfig.CORE_DEPENDENCY else files() })
    }

    private fun Project.addOutgoingVariant(
        sourceSet: CodeOwnersSourceSet,
        attributes: AttributeContainer
    ) = configurations.register("${sourceSet.name}CodeOwnersElements") {
        isCanBeResolved = false
        isCanBeConsumed = true
        description = "CODEOWNERS information for the sourceSet $name"
        attributes {
            from(
                attributes,
                Usage.USAGE_ATTRIBUTE,
                Category.CATEGORY_ATTRIBUTE,
                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                Bundling.BUNDLING_ATTRIBUTE
            )
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(ARTIFACT_TYPE_CODEOWNERS))
            attribute(ARTIFACT_TYPE_ATTRIBUTE, ARTIFACT_TYPE_CODEOWNERS)
        }
        outgoing {
            artifacts(sourceSet.enabled.map { enabled ->
                if (enabled) listOf(sourceSet.generateTask.map { it.mappedCodeOwnersFile })
                else emptyList()
            })
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
    }

    @Suppress("UNCHECKED_CAST")
    private fun <Type : AttributeContainer> Type.from(
        other: HasAttributes,
        vararg except: Attribute<*>,
    ) = apply {
        (other.attributes.keySet() - except.toSet()).forEach {
            attribute(it as Attribute<Any>, other.attributes.getAttribute(it)!!)
        }
    }

    private fun Provider<Boolean>.and(vararg others: Provider<Boolean>) =
        sequenceOf(this, *others).reduce { acc, it -> acc.zip(it, Boolean::and) }

}
