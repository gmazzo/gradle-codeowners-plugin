package io.github.gmazzo.codeowners

import org.gradle.api.provider.Property

interface CodeOwnersKotlinTargetExtension {

    /**
     * If it should compute the code owners for this compilation target
     */
    val enabled: Property<Boolean>

}
