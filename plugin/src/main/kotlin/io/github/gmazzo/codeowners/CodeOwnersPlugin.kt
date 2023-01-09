package io.github.gmazzo.codeowners

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.internal.tasks.ProcessJavaResTask
import io.github.gmazzo.codeowners.CodeOwnersCompatibilityRule.Companion.ARTIFACT_TYPE_CODEOWNERS
import io.github.gmazzo.codeowners.plugin.BuildConfig
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
import org.gradle.api.plugins.JvmEcosystemPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.*

class CodeOwnersPlugin : Plugin<Project> {

    private companion object {
        const val EXTENSION_NAME = "codeOwners"
    }

    override fun apply(target: Project): Unit = with(target) {
        rootProject.apply<CodeOwnersPlugin>()
        apply<JvmEcosystemPlugin>()

        val extension = createExtension()
        val sourceSets = createSourceSets(extension)

        bindSourceSets(sourceSets)
        addCoreDependency()
        setupAndroidSupport(sourceSets)
        setupArtifactTransform()
    }

    private fun Project.createExtension() = when (project) {
        rootProject -> extensions.create<CodeOwnersExtension>(EXTENSION_NAME).apply {
            rootDirectory
                .convention(layout.projectDirectory)
                .finalizeValueOnRead()

            codeOwnersFile
                .convention(layout.file(provider {
                    files(
                        "CODEOWNERS",
                        ".github/CODEOWNERS",
                        ".gitlab/CODEOWNERS",
                        "docs/CODEOWNERS",
                    ).asFileTree.singleFile
                }))
                .finalizeValueOnRead()

            codeOwners
                .value(codeOwnersFile.asFile.map { file -> file.useLines { CodeOwnersFile(it) } })
                .apply { disallowChanges() }
                .finalizeValueOnRead()
        }

        else -> rootProject.the<CodeOwnersExtension>().also { extensions.add(EXTENSION_NAME, it) }
    }

    private fun Project.createSourceSets(
        extension: CodeOwnersExtension,
    ) = objects.domainObjectContainer(CodeOwnersSourceSet::class) { name ->
        val ss = objects.sourceDirectorySet(name, "$name codeOwners sources")

        val prefix = when (name) {
            SourceSet.MAIN_SOURCE_SET_NAME -> ""
            else -> name.capitalized()
        }

        val runtimeResources = configurations.maybeCreate("codeOwners${prefix}Resources").apply {
            isCanBeConsumed = false
            isCanBeResolved = true
            isVisible = false
        }

        val generateTask = tasks.register<CodeOwnersTask>("generate${prefix}CodeOwnersResources") {
            codeOwners.value(extension.codeOwners)
            rootDirectory.value(extension.rootDirectory)
            sourceFiles.from(ss)
            runtimeClasspathResources.from(runtimeResources.incoming.artifactView {
                it.isLenient = true
                it.attributes.attribute(ARTIFACT_TYPE_ATTRIBUTE, ARTIFACT_TYPE_CODEOWNERS)
            }.files)
        }

        ss.destinationDirectory.value(layout.buildDirectory.dir("generated/codeOwners/${ss.name}"))
        ss.compiledBy(generateTask, CodeOwnersTask::outputDirectory)

        CodeOwnersSourceSet(ss, generateTask, runtimeResources)
    }

    private fun Project.bindSourceSets(
        sourceSets: NamedDomainObjectContainer<CodeOwnersSourceSet>,
    ) = the<SourceSetContainer>().configureEach { ss ->
        val sources = sourceSets.maybeCreate(ss.name)
        sources.source(ss.java)
        sources.runtimeResources.extendsFrom(configurations[ss.runtimeClasspathConfigurationName])

        ss.resources.srcDir(sources.generateTask)
        ss.extensions.add(SourceDirectorySet::class.java, "codeOwners", sources.sources)

        plugins.withId("kotlin") {
            val kotlin: SourceDirectorySet by ss.extensions

            sources.source(kotlin)
        }

        plugins.withId("groovy") {
            val groovy: SourceDirectorySet by ss.extensions

            sources.source(groovy)
        }
    }

    private fun Project.addCoreDependency() = configurations.maybeCreate(IMPLEMENTATION_CONFIGURATION_NAME).also {
        dependencies {
            it(BuildConfig.CORE_DEPENDENCY)
        }
    }

    private fun Project.setupAndroidSupport(
        sourceSets: NamedDomainObjectContainer<CodeOwnersSourceSet>,
    ) = plugins.withId("com.android.base") {
        val androidComponents: AndroidComponentsExtension<*, *, *> by extensions

        androidComponents.onVariants { variant ->
            val sources = sourceSets.maybeCreate(variant.name)
            sources.srcDir(listOfNotNull(variant.sources.java?.all, variant.sources.kotlin?.all))
            sources.runtimeResources.extendsFrom(variant.runtimeConfiguration)

            variant.packaging.resources.merges.add("**/*.codeowners")

            // TODO there is no `variant.sources.resources.addGeneratedSourceDirectory` DSL for this?
            afterEvaluate {
                tasks.named<ProcessJavaResTask>("process${variant.name.capitalized()}JavaRes") {
                    from(sources.generateTask.map(CodeOwnersTask::outputDirectory))
                }
            }
        }
    }

    private fun Project.setupArtifactTransform() = dependencies {
        attributesSchema.attribute(ARTIFACT_TYPE_ATTRIBUTE)
            .compatibilityRules.add(CodeOwnersCompatibilityRule::class.java)

        registerTransform(CodeOwnersTransform::class) {
            it.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, JAR_TYPE)
            it.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, ARTIFACT_TYPE_CODEOWNERS)
        }
    }

    private class CodeOwnersSourceSet(
        val sources: SourceDirectorySet,
        val generateTask: TaskProvider<CodeOwnersTask>,
        val runtimeResources: Configuration,
    ) : SourceDirectorySet by sources

}
