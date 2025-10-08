package io.github.gmazzo.codeowners

import java.io.Serializable
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

@JvmDefaultWithoutCompatibility
interface CodeOwnersExtensionBase<SourceSet : CodeOwnersSourceSet> {

    /**
     * The base path of the entries on the CODEOWNERS file
     */
    val rootDirectory: DirectoryProperty

    /**
     * The path to the CODEOWNERS file
     */
    val codeOwnersFile: RegularFileProperty

    /**
     * An optional renamer function to be applied to the owners on the CODEOWNERS file.
     */
    val codeOwnersRenamer: Property<Renamer>

    /**
     * Sets a renamer function to be applied to the owners on the CODEOWNERS file.
     */
    fun codeOwnersRenamer(renamer: Renamer)

    /**
     * The collecting of CodeOwners source sets.
     * This is usually a mirror of:
     * - [SourceSet]s for Java projects
     * - [com.android.build.api.variant.Component]s variants (include test and androidTest) for Android projects
     * - [org.jetbrains.kotlin.gradle.plugin.KotlinCompilation]s for Kotlin projects
     */
    val sourceSets: NamedDomainObjectContainer<SourceSet>

    /**
     * The directory where code owners reports are generated.
     */
    val reportsDirectory: DirectoryProperty

    /**
     * Access the reports produced by its [CodeOwnersReportTask] task
     */
    @get:Nested
    val reports: CodeOwnersReports

    /**
     * Configures the reports produced by its [CodeOwnersReportTask] task
     */
    fun reports(action: Action<CodeOwnersReports>) {
        action.execute(reports)
    }

    fun interface Renamer : Serializable {

        fun rename(owner: String): String

    }

}
