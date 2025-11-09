package io.github.gmazzo.codeowners

import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection

public interface CodeOwnersSourceSet : Named {

    /**
     * The source files collection to be analyzed by the CodeOwners tasks.
     *
     * Usually you don't need to feed them manually, as they are automatically collected from the build
     */
    public val sources: ConfigurableFileCollection

    /**
     * The compiled classes files collection to be analyzed by the CodeOwners tasks.
     *
     * Usually you don't need to feed them manually, as they are automatically collected from the build
     */
    public val classes: ConfigurableFileCollection

    /**
     * The external mapping files (.codeowners) collection to be analyzed by the CodeOwners tasks.
     *
     * Usually you don't need to feed them manually, as they are automatically collected from the build
     */
    public val mappings: ConfigurableFileCollection

}
