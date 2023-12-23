package io.github.gmazzo.codeowners

import org.gradle.api.provider.Property

interface CodeOwnersInspectDependencies {

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
