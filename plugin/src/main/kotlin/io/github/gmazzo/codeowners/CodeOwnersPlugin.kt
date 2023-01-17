package io.github.gmazzo.codeowners

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Component
import com.android.build.api.variant.HasAndroidTest
import io.github.gmazzo.codeowners.CodeOwnersCompatibilityRule.Companion.ARTIFACT_TYPE_CODEOWNERS
import io.github.gmazzo.codeowners.plugin.BuildConfig
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*

class CodeOwnersPlugin : Plugin<Project> {

    private val extensionName = Component::codeOwners.name

    private val attributeArtifactType = Attribute.of("artifactType", String::class.java)

    override fun apply(target: Project): Unit = with(target) {
        rootProject.apply<CodeOwnersPlugin>()

        val extension = createExtension()
        val sourceSets = createSourceSets(extension)

        bindSourceSets(sourceSets)
        setupAndroidSupport(sourceSets)
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
        }

        else -> rootProject.the<CodeOwnersExtension>().also { extensions.add(extensionName, it) }
    }

    private fun Project.createSourceSets(
        extension: CodeOwnersExtension,
    ) = objects.domainObjectContainer(CodeOwnersSourceSet::class) { name ->
        val ss = objects.sourceDirectorySet(name, "$name codeOwners sources")

        val prefix = when (name) {
            SourceSet.MAIN_SOURCE_SET_NAME -> ""
            else -> name.capitalize()
        }

        val generateTask = tasks.register("generate${prefix}CodeOwnersResources", CodeOwnersTask::class.java, ss)
        generateTask {
            codeOwners.value(extension.codeOwners)
            rootDirectory.value(extension.rootDirectory)
        }

        ss.destinationDirectory.value(layout.buildDirectory.dir("generated/codeOwners/${ss.name}"))
        ss.compiledBy(generateTask, CodeOwnersTask::outputDirectory)

        objects.newInstance<CodeOwnersSourceSetImpl>(ss, generateTask).apply {
            enabled.convention(true).finalizeValueOnRead()
        }
    }

    private fun Project.bindSourceSets(
        sourceSets: NamedDomainObjectContainer<CodeOwnersSourceSet>,
    ) = plugins.withId("java-base") {
        the<SourceSetContainer>().configureEach { ss ->
            val sources = sourceSets.maybeCreate(ss.name)
            sources.source(ss.allJava)
            sources.generateTask {
                runtimeClasspathResources.from(configurations[ss.runtimeClasspathConfigurationName].codeOwners)
            }
            addCodeDependency(ss.implementationConfigurationName)

            ss.extensions.add(CodeOwnersSourceSet::class.java, extensionName, sources)
            tasks.named<AbstractCopyTask>(ss.processResourcesTaskName).addResources(sources)
        }
    }

    private fun Project.setupAndroidSupport(
        sourceSets: NamedDomainObjectContainer<CodeOwnersSourceSet>,
    ) = plugins.withId("com.android.base") {
        val androidComponents: AndroidComponentsExtension<*, *, *> by extensions

        fun bind(
            component: Component,
            defaultsTo: CodeOwnersSourceSet? = null,
        ): CodeOwnersSourceSet {
            val sources = sourceSets.maybeCreate(component.name).also(component::codeOwners.setter)
            sources.srcDir(listOfNotNull(component.sources.java?.all, component.sources.kotlin?.all))
            sources.generateTask {
                runtimeClasspathResources.from(component.runtimeConfiguration.codeOwners)
            }
            addCodeDependency(component.compileConfiguration.name)

            // TODO there is no `variant.sources.resources.addGeneratedSourceDirectory` DSL for this?
            afterEvaluate {
                tasks.named<AbstractCopyTask>("process${component.name.capitalize()}JavaRes")
                    .addResources(sources)
            }

            if (defaultsTo != null) {
                sources.enabled.convention(defaultsTo.enabled)
            }
            return sources
        }

        androidComponents.onVariants { variant ->
            variant.packaging.resources.merges.add("**/*.codeowners")

            val sources = bind(variant)
            variant.unitTest?.let { bind(component = it, defaultsTo = sources) }
            (variant as? HasAndroidTest)?.androidTest?.let { bind(component = it, defaultsTo = sources) }
        }
    }

    private val Configuration.codeOwners
        get() = incoming
            .artifactView { it.attributes.attribute(attributeArtifactType, ARTIFACT_TYPE_CODEOWNERS) }
            .files

    private fun TaskProvider<out AbstractCopyTask>.addResources(sources: CodeOwnersSourceSet) = configure { task ->
        task.from(sources.enabled.map {
            if (it) sources.generateTask.map(CodeOwnersTask::outputDirectory) else emptyList<Any>()
        })
    }

    private val Project.includeCoreDependency
        get() = findProperty("codeowners.default.dependency")?.toString()?.toBoolean() != false

    private fun Project.addCodeDependency(configurationName: String) {
        if (includeCoreDependency) {
            dependencies.add(configurationName, BuildConfig.CORE_DEPENDENCY)

        } else {
            dependencies.constraints.add(configurationName, BuildConfig.CORE_DEPENDENCY)
        }
    }

    private fun Project.setupArtifactTransform() = dependencies {
        attributesSchema.attribute(attributeArtifactType)
            .compatibilityRules.add(CodeOwnersCompatibilityRule::class.java)

        registerTransform(CodeOwnersTransform::class) {
            it.from.attribute(attributeArtifactType, JAR_TYPE)
            it.to.attribute(attributeArtifactType, ARTIFACT_TYPE_CODEOWNERS)
        }
    }

}
