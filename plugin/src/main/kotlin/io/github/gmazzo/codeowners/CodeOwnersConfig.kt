package io.github.gmazzo.codeowners

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.util.PatternFilterable

interface CodeOwnersConfig {

    /**
     * Whether to filter which classes to process by an include [PatternFilterable] pattern.
     * Empty implies `all`
     */
    val includes: SetProperty<String>

    /**
     * Whether to filter which classes to process by an exclude [PatternFilterable] pattern.
     * Empty implies `all`
     */
    val excludes: SetProperty<String>

    /**
     * Whether to inspect or not runtime dependencies of the module to detect package collisions and generate more
     * accurate ownership information.
     *
     * Default to [DependenciesMode.LOCAL_PROJECTS] which has the best performance/accuracy ratio.
     */
    val inspectDependencies: Property<DependenciesMode>

    enum class DependenciesMode {

        /**
         * Inspects both local (projects) and remote (modules) dependencies.
         *
         * Expensive operation as all dependencies has to be scanned
         */
        ALL,

        /**
         * Only lo local projects are considered.
         *
         * This mode has pretty much no performance impact, as resources folder is already available in the build
         */
        LOCAL_PROJECTS,

        /**
         * Do not consider any dependency.
         *
         * Fastest mode, but less accurate (when having the same JVM package)
         */
        NONE,

    }

}
