package io.github.gmazzo.codeowners

import org.gradle.api.provider.Property

interface CodeOwnersJVMExtension : CodeOwnersExtension<CodeOwnersJVMSourceSet>, CodeOwnersInspectDependencies {

    /**
     * Where if code ownership info should be added as Java resources to be available at runtime.
     */
    val enabled: Property<Boolean>

}
