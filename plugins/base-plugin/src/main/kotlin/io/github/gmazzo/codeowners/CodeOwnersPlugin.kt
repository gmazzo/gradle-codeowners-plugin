package io.github.gmazzo.codeowners

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ScopedArtifacts
import io.github.gmazzo.codeowners.KotlinSupport.Companion.codeOwnersSourceSetName
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.reportTask
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import javax.inject.Inject

open class CodeOwnersPlugin<Extension : CodeOwnersExtension<*>>(
    private val extensionClass: Class<out Extension>,
) : Plugin<Project> {

    @Inject
    @Suppress("UNCHECKED_CAST")
    constructor() : this(DefaultExtension::class.java as Class<out Extension>)

    companion object {
        const val TASK_GROUP = "CodeOwners"
    }

    protected open fun Project.configureExtension(extension: Extension) {}
    protected open fun Project.configureByAndroidVariants(extension: Extension) {}
    protected open fun Project.configureByKotlinTargets(extension: Extension) {}
    protected open fun Project.configureBySourceSet(extension: Extension) {}

    override fun apply(target: Project): Unit = with(target) {
        GradleVersion.version("7.5").let { required ->
            check(GradleVersion.current() >= required) {
                "`io.github.gmazzo.codeowners` plugin requires $required or higher"
            }
        }

        val extension = extensions.create("codeOwners", extensionClass)

        configureExtensionInternal(extension)
        configureSourceSets(extension)
        configureExtension(extension)

        // configures collaborating plugin's specifics
        plugins.withId("java") {
            configureBySourceSetInternal(extension)
            configureBySourceSet(extension)
        }
        plugins.withId("org.jetbrains.kotlin.jvm", "org.jetbrains.kotlin.multiplatform") {
            configureByKotlinTargetsInternal(extension)
            configureByKotlinTargets(extension)
        }
        plugins.withId("com.android.base") {
            configureByAndroidVariantsInternal(extension)
            configureByAndroidVariants(extension)
        }
    }

    @Suppress("SameParameterValue")
    private fun PluginContainer.withId(vararg pluginIds: String, action: Action<Plugin<*>>) =
        pluginIds.forEach { withId(it, action) }

    private fun Project.configureExtensionInternal(extension: Extension) = with(extension) {
        val parentExtension = generateSequence(parent) { it.parent }
            .mapNotNull { it.extensions.findByType(CodeOwnersExtension::class.java) }
            .firstOrNull()

        rootDirectory
            .valueIfNotNull(parentExtension?.rootDirectory)
            .convention(layout.projectDirectory)
            .finalizeValueOnRead()

        val defaultLocations = files(
            "CODEOWNERS",
            ".github/CODEOWNERS",
            ".gitlab/CODEOWNERS",
            "docs/CODEOWNERS",
        )

        val defaultFile = lazy {
            defaultLocations.asFileTree.singleOrNull() ?: error(defaultLocations.joinToString(
                prefix = "No CODEOWNERS file found! Default locations:\n",
                separator = "\n"
            ) { "- ${it.toRelativeString(rootDir)}" })
        }

        codeOwnersFile
            .valueIfNotNull(parentExtension?.codeOwnersFile)
            .convention(layout.file(provider(defaultFile::value)))
            .finalizeValueOnRead()
    }

    private fun Project.configureSourceSets(extension: Extension) {
        val reportAllTask by lazy {
            tasks.register<CodeOwnersReportTask>("codeOwnersReport") {
                group = TASK_GROUP
                description = "Generates CODEOWNERS report for all classes of this project"

                rootDirectory.set(extension.rootDirectory)
                codeOwnersFile.set(extension.codeOwnersFile)
                codeOwnersReportHeader.set("CodeOwners of module '${project.path}'\n")
                codeOwnersReportFile.set(layout.buildDirectory.file("reports/codeOwners/${project.name}.codeowners"))
            }
        }

        extension.sourceSets.configureEach ss@{
            check(name.isNotBlank()) { "Source set name cannot be empty" }

            reportAllTask.configure {
                sources.from(this@ss.sources)
                classes.from(this@ss.classes)
            }

            reportTask = tasks.register<CodeOwnersReportTask>("${this@ss.name}CodeOwnersReport") {
                group = TASK_GROUP
                description = "Generates CODEOWNERS report for '${this@ss.name}'"

                sources.from(this@ss.sources)
                classes.from(this@ss.classes)
                rootDirectory.set(extension.rootDirectory)
                codeOwnersFile.set(extension.codeOwnersFile)
                codeOwnersReportHeader.set("CodeOwners of module '${project.path}' (source set '${this@ss.name}')\n")
                codeOwnersReportFile.set(layout.buildDirectory.file("reports/codeOwners/${project.name}-${this@ss.name}.codeowners"))
            }
        }
    }

    private fun Project.configureBySourceSetInternal(extension: Extension) =
        the<SourceSetContainer>().configureEach {
            val sourceSet = extension.sourceSets.maybeCreate(name)

            sourceSet.sources.from(allSource.sourceDirectories)
            sourceSet.classes.from(output.classesDirs)
        }

    private fun Project.configureByKotlinTargetsInternal(extension: Extension) =
        KotlinSupport(this).configureTargets {
            if (this !is KotlinMetadataTarget) {
                compilations.configureEach {
                    val sourceSet = extension.sourceSets.maybeCreate(codeOwnersSourceSetName)

                    sourceSet.sources.from(provider { allKotlinSourceSets.map { it.kotlin.sourceDirectories } })
                    sourceSet.classes.from(output.classesDirs)
                }
            }
        }

    private fun Project.configureByAndroidVariantsInternal(extension: Extension) = with(AndroidSupport(project)) {
        configureComponents {
            val kotlinAwareSourceSetName =
                if (plugins.hasPlugin("org.jetbrains.kotlin.multiplatform"))
                    the<KotlinTargetsContainer>()
                        .targets.withType<KotlinAndroidTarget>()
                        .firstOrNull()?.name?.let { "${it}${name.capitalized()}" }
                else null

            val sourceSet = extension.sourceSets.maybeCreate(kotlinAwareSourceSetName ?: name)
            val classes = objects.listProperty<RegularFile>()
            val jars = objects.listProperty<Directory>()

            sourceSet.sources.from(sources.java?.all, sources.kotlin?.all)
            sourceSet.classes.from(classes, jars)

            artifacts.forScope(ScopedArtifacts.Scope.PROJECT)
                .use(sourceSet.reportTask)
                .toGet(ScopedArtifact.CLASSES, { classes }, { jars })
        }
    }

    private val Project.pathAsFileName
        get() = path.removePrefix(":").replace(':', '-')

    private fun <Type, PropertyOfType : Property<Type>> PropertyOfType.valueIfNotNull(
        provider: Provider<Type>?,
    ): PropertyOfType = apply {
        if (provider != null) {
            value(provider)
        }
    }

    internal interface DefaultExtension : CodeOwnersExtension<CodeOwnersSourceSet>

}
