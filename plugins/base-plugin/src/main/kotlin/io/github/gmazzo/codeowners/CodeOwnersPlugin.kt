package io.github.gmazzo.codeowners

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ScopedArtifacts
import io.github.gmazzo.codeowners.KotlinSupport.Companion.codeOwnersSourceSetName
import javax.inject.Inject
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

open class CodeOwnersPlugin<Extension : CodeOwnersExtensionBase<*>>(
    private val extensionClass: Class<out Extension>,
) : Plugin<Project> {

    @Inject
    @Suppress("UNCHECKED_CAST", "unused")
    constructor() : this(CodeOwnersExtension::class.java as Class<out Extension>)

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

        val reportAllTask = tasks.register<CodeOwnersReportTask>("codeOwnersReport") {
            group = TASK_GROUP
            description = "Generates CODEOWNERS report for all classes of this project"

            rootDirectory.set(extension.rootDirectory)
            codeOwnersFile.set(extension.codeOwnersFile)
            codeOwnersReportHeader.set("CodeOwners of module '${project.path}'\n")
            codeOwnersReportFile.set(layout.buildDirectory.file("reports/codeOwners/${project.name}.codeowners"))
        }

        configureExtensionInternal(extension)
        configureSourceSets(extension, reportAllTask)
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
            configureByAndroidVariantsInternal(extension, reportAllTask)
            configureByAndroidVariants(extension)
        }
    }

    @Suppress("SameParameterValue")
    private fun PluginContainer.withId(vararg pluginIds: String, action: Action<Plugin<*>>) =
        pluginIds.forEach { withId(it, action) }

    private fun Project.configureExtensionInternal(extension: Extension) = with(extension) {
        val parentExtension = generateSequence(parent) { it.parent }
            .mapNotNull { it.extensions.findByType(CodeOwnersExtensionBase::class.java) }
            .firstOrNull()

        rootDirectory
            .convention(parentExtension?.rootDirectory ?: gitRoot)
            .finalizeValueOnRead()

        codeOwnersFile
            .convention(parentExtension?.codeOwnersFile ?: rootDirectory.defaultCodeOwnersFile)
            .finalizeValueOnRead()
    }

    private val Provider<Directory>.defaultCodeOwnersFile get() = map { root ->
        val locations = listOf(
            "CODEOWNERS",
            ".github/CODEOWNERS",
            ".gitlab/CODEOWNERS",
            "docs/CODEOWNERS",
        )
        val existingLocations = locations.mapNotNull { path -> root.file(path).takeIf { it.asFile.exists() } }

        when (existingLocations.size) {
            1 -> existingLocations.first()
            0 -> error(locations.joinToString(prefix = "No CODEOWNERS file found! Default locations:\n", separator = "\n") { "- $it" })
            else -> error(existingLocations.joinToString(prefix = "Multiple CODEOWNERS file found! Locations:\n", separator = "\n") { "- $it" })
        }
    }

    private val Project.gitRoot: Provider<Directory>
        get() {
            val rootDir = rootProject.layout.projectDirectory

            return providers
                .exec { commandLine("git", "rev-parse", "--show-toplevel") }
                .standardOutput.asText.map { if (it.isNotBlank()) rootDir.dir(it.trim()) else rootDir }
        }

    private fun Project.configureSourceSets(
        extension: Extension,
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
        extension: Extension,
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

}
