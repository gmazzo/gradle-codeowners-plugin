package io.github.gmazzo.codeowners

import org.gradle.api.provider.Property

interface CodeOwnersJVMSourceSet : CodeOwnersSourceSet, CodeOwnersInspectDependencies {

    /**
     * If it should compute the code owners for this source set
     */
    val enabled: Property<Boolean>

}
