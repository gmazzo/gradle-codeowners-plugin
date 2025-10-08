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
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.reportTask
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget

open class CodeOwnersPlugin<Extension : CodeOwnersExtensionBaseInternal<*>> : Plugin<Project> {

    companion object {
        const val TASK_GROUP = "CodeOwners"
        private const val EXTENSION_NAME = "codeOwners"
    }

    protected open val extensionClass: Class<out CodeOwnersExtensionBase<*>> = CodeOwnersExtension::class.java
    protected open val extensionClassImpl: Class<out CodeOwnersExtensionBaseInternal<*>> =
        CodeOwnersExtensionInternal::class.java

    protected open fun Project.configureExtension() {}
    protected open fun Project.configureByAndroidVariants() {}
    protected open fun Project.configureByKotlinTargets() {}
    protected open fun Project.configureBySourceSet() {}

    protected lateinit var extension: Extension
        private set

    override fun apply(target: Project): Unit = with(target) {
        GradleVersion.version("7.5").let { required ->
            check(GradleVersion.current() >= required) {
                "`io.github.gmazzo.codeowners` plugin requires $required or higher"
            }
        }

        @Suppress("UNCHECKED_CAST")
        extension = extensions.create(
            extensionClass as Class<CodeOwnersExtensionBase<*>>,
            EXTENSION_NAME,
            extensionClassImpl as Class<Extension>,
            lazy { tasks.register<CodeOwnersRenameTask>("renameCodeOwners") },
        ) as Extension

        val reportAllTask = tasks.register<CodeOwnersReportTask>("codeOwnersReport") {
            group = TASK_GROUP
            description = "Generates CODEOWNERS report for all classes of this project"

            rootDirectory.set(extension.rootDirectory)
            codeOwnersFile.set(extension.renamedCodeOwnersFile)
            codeOwnersReportHeader.set("CodeOwners of module '${project.path}'\n")
            codeOwnersReportFile.set(layout.buildDirectory.file("reports/codeOwners/${project.name}.codeowners"))
        }

        configureExtensionInternal()
        configureSourceSets(reportAllTask)
        configureExtension()

        // configures collaborating plugin's specifics
        plugins.withId("java") {
            configureBySourceSetInternal()
            configureBySourceSet()
        }
        plugins.withId("org.jetbrains.kotlin.jvm", "org.jetbrains.kotlin.multiplatform") {
            configureByKotlinTargetsInternal()
            configureByKotlinTargets()
        }
        plugins.withId("com.android.base") {
            configureByAndroidVariantsInternal( reportAllTask)
            configureByAndroidVariants()
        }
    }

    @Suppress("SameParameterValue")
    private fun PluginContainer.withId(vararg pluginIds: String, action: Action<Plugin<*>>) =
        pluginIds.forEach { withId(it, action) }

    private fun Project.configureExtensionInternal() = with(extension) {
        val parentExtension = generateSequence(parent) { it.parent }
            .mapNotNull { it.extensions.findByName(EXTENSION_NAME) as CodeOwnersExtensionBaseInternal<*>? }
            .firstOrNull()

        rootDirectory
            .convention(parentExtension?.rootDirectory.orElse(gitRoot))
            .finalizeValueOnRead()

        codeOwnersFile
            .convention( parentExtension?.codeOwnersFile.orElse(rootDirectory.defaultCodeOwnersFile))
            .finalizeValueOnRead()

        codeOwnersRenamer
            .finalizeValueOnRead()

        renamedCodeOwnersFile
            .convention(parentExtension?.renamedCodeOwnersFile.orElse(codeOwnersFile))
            .finalizeValueOnRead()

        renamedCodeOwnersFileUntracked
            .convention(parentExtension?.renamedCodeOwnersFileUntracked.orElse(codeOwnersFile))
            .finalizeValueOnRead()
    }

    private val Provider<Directory>.defaultCodeOwnersFile
        get() = map { root ->
            val locations = listOf(
                "CODEOWNERS",
                ".github/CODEOWNERS",
                ".gitlab/CODEOWNERS",
                "docs/CODEOWNERS",
            ).map { path -> root.file(path) }
            val existingLocations = locations.mapNotNull { it.takeIf { it.asFile.isFile } }

            when (existingLocations.size) {
                1 -> existingLocations.first()
                0 -> error(
                    locations.joinToString(
                        prefix = "No CODEOWNERS file found! Default locations:\n",
                        separator = "\n"
                    ) { "- $it" })

                else -> error(
                    existingLocations.joinToString(
                        prefix = "Multiple CODEOWNERS file found! Locations:\n",
                        separator = "\n"
                    ) { "- $it" })
            }
        }

    private val Project.gitRoot: Provider<Directory>
        get() {
            val rootDir = rootProject.layout.projectDirectory

            return providers
                .exec { commandLine("git", "rev-parse", "--show-toplevel"); isIgnoreExitValue = true }
                .standardOutput.asText.map { if (it.isNotBlank()) rootDir.dir(it.trim()) else rootDir }
        }

    private fun Project.configureSourceSets(
        reportAllTask: TaskProvider<CodeOwnersReportTask>,
    ) {
        extension.sourceSets.configureEach ss@{
            check(name.isNotBlank()) { "Source set name cannot be empty" }

            reportAllTask.configure {
                sources.from(this@ss.sources)
                classes.from(this@ss.classes)
                mappings.from(this@ss.mappings)
            }

            reportTask =
                tasks.register<CodeOwnersReportTask>("codeOwners${this@ss.name.replaceFirstChar { it.uppercase() }}Report") {
                    group = TASK_GROUP
                    description = "Generates CODEOWNERS report for '${this@ss.name}'"

                    sources.from(this@ss.sources)
                    classes.from(this@ss.classes)
                    mappings.from(this@ss.mappings)
                    rootDirectory.set(extension.rootDirectory)
                    codeOwnersFile.set(extension.renamedCodeOwnersFile)
                    codeOwnersReportHeader.set("CodeOwners of module '${project.path}' (source set '${this@ss.name}')\n")
                    codeOwnersReportFile.set(layout.buildDirectory.file("reports/codeOwners/${project.name}-${this@ss.name}.codeowners"))
                }
        }
    }

    private fun Project.configureBySourceSetInternal() =
        the<SourceSetContainer>().configureEach {
            val sourceSet = extension.sourceSets.maybeCreate(name)

            sourceSet.sources.from(allSource.sourceDirectories)
            sourceSet.classes.from(output.classesDirs)
        }

    private fun Project.configureByKotlinTargetsInternal() =
        KotlinSupport(this).configureTargets @JvmSerializableLambda {
            if (this !is KotlinMetadataTarget) {
                compilations.configureEach {
                    val sourceSet = extension.sourceSets.maybeCreate(codeOwnersSourceSetName)

                    sourceSet.sources.from(provider { allKotlinSourceSets.map { it.kotlin.sourceDirectories } })
                    sourceSet.classes.from(output.classesDirs)
                }
            }
        }

    private fun Project.configureByAndroidVariantsInternal(
        reportAllTask: TaskProvider<CodeOwnersReportTask>,
    ) = with(AndroidSupport(project)) {
        configureComponents @JvmSerializableLambda {
            val kotlinAwareSourceSetName =
                if (plugins.hasPlugin("org.jetbrains.kotlin.multiplatform"))
                    the<KotlinTargetsContainer>()
                        .targets.withType<KotlinAndroidTarget>()
                        .firstOrNull()?.name?.let { "${it}${name.replaceFirstChar { it.uppercase() }}" }
                else null

            val sourceSet = extension.sourceSets.maybeCreate(kotlinAwareSourceSetName ?: name)
            val classes = objects.listProperty<RegularFile>()
            val jars = objects.listProperty<Directory>()

            sourceSet.sources.from(sources.java?.all, sources.kotlin?.all)
            sourceSet.classes.from(classes, jars)

            artifacts.forScope(ScopedArtifacts.Scope.PROJECT)
                .use(reportAllTask)
                .toGet(ScopedArtifact.CLASSES, { classes }, { jars })

            artifacts.forScope(ScopedArtifacts.Scope.PROJECT)
                .use(sourceSet.reportTask)
                .toGet(ScopedArtifact.CLASSES, { classes }, { jars })
        }
    }

    private fun <Type> Provider<Type>?.orElse(
        provider: Provider<Type>,
    ): Provider<Type> = this?.orElse(provider) ?: provider

}
