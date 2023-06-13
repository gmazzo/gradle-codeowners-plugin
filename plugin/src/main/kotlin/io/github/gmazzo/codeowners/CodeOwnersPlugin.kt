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
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE
import org.gradle.api.attributes.Attribute
import org.gradle.api.provider.Provider
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

    private val attributeArtifactType = Attribute.of("artifactType", String::class.java)

    override fun apply(target: Project): Unit = with(target) {
        rootProject.apply<CodeOwnersPlugin>()

        val extension = createExtension()
        val sourceSets = createSourceSets(extension)

        bindSourceSets(extension, sourceSets)
        setupAndroidSupport(extension, sourceSets)
        setupArtifactTransform()
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
        the<SourceSetContainer>().configureEach { ss ->
            val sourceSet = sourceSets.maybeCreate(ss.name)
            sourceSet.generateTask {
                sources.from(provider { ss.allJava.srcDirs }) // will contain srcDirs of groovy, kotlin, etc. too
                addDependencies(sourceSet, configurations[ss.runtimeClasspathConfigurationName])
            }
            addCodeDependency(extension, sourceSet, ss.implementationConfigurationName)

            ss.extensions.add(CodeOwnersSourceSet::class.java, extensionName, sourceSet)
            tasks.named<AbstractCopyTask>(ss.processResourcesTaskName)
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
                addDependencies(sourceSet, component.runtimeConfiguration)
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
        }
    }

    private fun CodeOwnersTask.addDependencies(
        sourceSet: CodeOwnersSourceSet,
        configuration: Configuration,
    ) {
        val mode = sourceSet.inspectDependencies.get()
        if (mode == InspectDependencies.Mode.NONE) return

        runtimeClasspath.from(configuration.incoming.artifactView { view ->
            view.attributes.attribute(attributeArtifactType, ARTIFACT_TYPE_CODEOWNERS)

            if (mode == InspectDependencies.Mode.LOCAL_PROJECTS) {
                view.componentFilter { it is ProjectComponentIdentifier }
            }
        }.files)
    }

    private fun TaskProvider<out AbstractCopyTask>.addResources(
        extension: CodeOwnersExtension,
        sources: CodeOwnersSourceSet
    ) = configure { task ->
        task.from(extension.addCodeOwnershipAsResources.and(sources.enabled).map {
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

    private fun Project.setupArtifactTransform() = dependencies {
        registerTransform(CodeOwnersTransform::class) {
            it.from.attribute(attributeArtifactType, JAR_TYPE)
            it.to.attribute(attributeArtifactType, ARTIFACT_TYPE_CODEOWNERS)
        }
    }

    private fun Provider<Boolean>.and(vararg others: Provider<Boolean>) =
        sequenceOf(this, *others).reduce { acc, it -> acc.zip(it, Boolean::and) }

}
