package io.github.gmazzo.codeowners

import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.jetbrains.annotations.Range

@JvmDefaultWithoutCompatibility
interface CodeOwnersReports {

    /**
     * The severity level to report unowned class files. An entry will be added per each.
     */
    @get:Input
    val unownedClassSeverity: Property<Severity>

    /**
     * The maximum percentage (`0..100`) of unowned class files allowed before the check fails.
     *
     * - `0` means no unowned files are allowed (default)
     * - `100` any number of files can be unowned (never fails)
     */
    @get:Input
    @get:Optional
    val failOnUnownedThreshold: Property<@Range(from = 0, to = 100) Float>

    /**
     * Fail the build if there are any unowned class files (same as setting `failOnUnownedThreshold` to `0`).
     */
    fun failOnUnowned() {
        failOnUnownedThreshold.value(0f).disallowChanges()
    }

    /**
     * Never fail the build due to unowned class files (same as setting `failOnUnownedThreshold` to `100`).
     */
    fun ignoreUnowned() {
        failOnUnownedThreshold.value(100f).disallowChanges()
    }

    @get:Nested
    val mappings: MappingsReport

    fun mappings(action: Action<MappingsReport>) {
        action.execute(mappings)
    }

    @get:Nested
    val html: HTMLReport

    fun html(action: Action<HTMLReport>) {
        action.execute(html)
    }

    @get:Nested
    val xml: Report

    fun xml(action: Action<Report>) {
        action.execute(xml)
    }

    @get:Nested
    val checkstyle: Report

    fun checkstyle(action: Action<Report>) {
        action.execute(checkstyle)
    }

    @get:Nested
    val sarif: Report

    fun sarif(action: Action<Report>) {
        action.execute(sarif)
    }

    interface Report {

        @get:Input
        val required: Property<Boolean>

        @get:OutputFile
        val outputLocation: RegularFileProperty

    }

    interface HTMLReport : Report {

        @get:Input
        @get:Optional
        val stylesheet: Property<String>

    }

    interface MappingsReport : Report {

        @get:Input
        @get:Optional
        val header: Property<String>

    }

    enum class Severity { INFO, WARNING, ERROR }

}
