package io.github.gmazzo.codeowners

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Component
import com.android.build.api.variant.HasAndroidTest
import com.android.build.gradle.internal.tasks.ProcessJavaResTask
import io.github.gmazzo.codeowners.CodeOwnersCompatibilityRule.Companion.ARTIFACT_TYPE_CODEOWNERS
import io.github.gmazzo.codeowners.plugin.BuildConfig
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE
import org.gradle.api.plugins.JvmEcosystemPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.*

class CodeOwnersPlugin : Plugin<Project> {

    private val extensionName = Component::codeOwners.name

    override fun apply(target: Project): Unit = with(target) {
        rootProject.apply<CodeOwnersPlugin>()
        apply<JvmEcosystemPlugin>()

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
            else -> name.capitalized()
        }

        val generateTask = tasks.register<CodeOwnersTask>("generate${prefix}CodeOwnersResources") {
            codeOwners.value(extension.codeOwners)
            rootDirectory.value(extension.rootDirectory)
            sourceFiles.from(ss)
        }

        ss.destinationDirectory.value(layout.buildDirectory.dir("generated/codeOwners/${ss.name}"))
        ss.compiledBy(generateTask, CodeOwnersTask::outputDirectory)

        objects.newInstance<CodeOwnersSourceSetImpl>(ss, generateTask).apply {
            includeAsResources.convention(true).finalizeValueOnRead()
            includeCoreDependency.convention(true).finalizeValueOnRead()
        }
    }

    private fun Project.bindSourceSets(
        sourceSets: NamedDomainObjectContainer<CodeOwnersSourceSet>,
    ) = plugins.withId("java-base") {
        the<SourceSetContainer>().configureEach {
            val sources = sourceSets.maybeCreate(it.name)
            sources.source(it.allJava)
            sources.generateTask {
                runtimeClasspathResources.from(configurations[it.runtimeClasspathConfigurationName].codeOwners)
            }

            addCoreDependency(sources, it.implementationConfigurationName)
            it.resources.srcDir(sources.includeAsResources.map { if (it) sources.generateTask else files() })
            it.extensions.add(CodeOwnersSourceSet::class.java, extensionName, sources)
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
            addCoreDependency(sources, component.compileConfiguration.name)

            // TODO there is no `variant.sources.resources.addGeneratedSourceDirectory` DSL for this?
            afterEvaluate {
                tasks.named<ProcessJavaResTask>("process${component.name.capitalized()}JavaRes") {
                    from(sources.includeAsResources
                        .map { if (it) sources.generateTask.map(CodeOwnersTask::outputDirectory) else files() })
                }
            }

            if (defaultsTo != null) {
                sources.includeAsResources.convention(defaultsTo.includeAsResources)
                sources.includeCoreDependency.convention(defaultsTo.includeCoreDependency)
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

    private fun Project.addCoreDependency(sources: CodeOwnersSourceSet, configuration: String) =
        dependencies.addProvider(
            configuration,
            sources.includeCoreDependency.map { if (it) dependencies.create(BuildConfig.CORE_DEPENDENCY) else files() }
        )

    private val Configuration.codeOwners
        get() = incoming
            .artifactView { it.attributes.attribute(ARTIFACT_TYPE_ATTRIBUTE, ARTIFACT_TYPE_CODEOWNERS) }
            .files

    private fun Project.setupArtifactTransform() = dependencies {
        attributesSchema.attribute(ARTIFACT_TYPE_ATTRIBUTE)
            .compatibilityRules.add(CodeOwnersCompatibilityRule::class.java)

        registerTransform(CodeOwnersTransform::class) {
            it.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, JAR_TYPE)
            it.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, ARTIFACT_TYPE_CODEOWNERS)
        }
    }

}
