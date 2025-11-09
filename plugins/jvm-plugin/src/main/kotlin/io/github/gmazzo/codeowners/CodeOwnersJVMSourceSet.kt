package io.github.gmazzo.codeowners

import org.gradle.api.provider.Property

public interface CodeOwnersJVMSourceSet : CodeOwnersSourceSet, CodeOwnersInspectDependencies {

    /**
     * If it should compute the code owners for this source set
     */
    public val enabled: Property<Boolean>

}
