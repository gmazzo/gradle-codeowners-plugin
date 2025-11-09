package io.github.gmazzo.codeowners

import org.gradle.api.provider.Property

public interface CodeOwnersJVMExtension : CodeOwnersExtensionBase<CodeOwnersJVMSourceSet>, CodeOwnersInspectDependencies {

    /**
     * Where if code ownership info should be added as Java resources to be available at runtime.
     */
    public val enabled: Property<Boolean>

}
