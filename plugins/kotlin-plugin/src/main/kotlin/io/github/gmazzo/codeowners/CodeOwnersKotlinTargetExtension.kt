package io.github.gmazzo.codeowners

import org.gradle.api.provider.Property

public interface CodeOwnersKotlinTargetExtension {

    /**
     * If it should compute the code owners for this compilation target
     */
    public val enabled: Property<Boolean>

}
