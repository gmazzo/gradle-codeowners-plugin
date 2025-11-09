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
public interface CodeOwnersReports {

    /**
     * The severity level to report unowned class files. An entry will be added per each.
     */
    @get:Input
    public val unownedClassSeverity: Property<Severity>

    /**
     * The maximum percentage (`0..100`) of unowned class files allowed before the check fails.
     *
     * - `0` means no unowned files are allowed (default)
     * - `100` any number of files can be unowned (never fails)
     */
    @get:Input
    @get:Optional
    public val failOnUnownedThreshold: Property<@Range(from = 0, to = 100) Float>

    /**
     * Fail the build if there are any unowned class files (same as setting `failOnUnownedThreshold` to `0`).
     */
    public fun failOnUnowned() {
        failOnUnownedThreshold.value(0f).disallowChanges()
    }

    /**
     * Never fail the build due to unowned class files (same as setting `failOnUnownedThreshold` to `100`).
     */
    public fun ignoreUnowned() {
        failOnUnownedThreshold.value(100f).disallowChanges()
    }

    @get:Nested
    public val mappings: MappingsReport

    public fun mappings(action: Action<MappingsReport>) {
        action.execute(mappings)
    }

    @get:Nested
    public val html: HTMLReport

    public fun html(action: Action<HTMLReport>) {
        action.execute(html)
    }

    @get:Nested
    public val xml: Report

    public fun xml(action: Action<Report>) {
        action.execute(xml)
    }

    @get:Nested
    public val checkstyle: Report

    public fun checkstyle(action: Action<Report>) {
        action.execute(checkstyle)
    }

    @get:Nested
    public val sarif: Report

    public fun sarif(action: Action<Report>) {
        action.execute(sarif)
    }

    public interface Report {

        @get:Input
        public val required: Property<Boolean>

        @get:OutputFile
        public val outputLocation: RegularFileProperty

    }

    public interface HTMLReport : Report {

        @get:Input
        @get:Optional
        public val stylesheet: Property<String>

    }

    public interface MappingsReport : Report {

        @get:Input
        @get:Optional
        public val header: Property<String>

    }

    public enum class Severity { INFO, WARNING, ERROR }

}
