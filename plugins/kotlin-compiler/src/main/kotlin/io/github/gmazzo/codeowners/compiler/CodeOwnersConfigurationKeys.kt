package io.github.gmazzo.codeowners.compiler

import java.io.File
import org.jetbrains.kotlin.config.CompilerConfigurationKey

public object CodeOwnersConfigurationKeys {
    public val CODEOWNERS_ROOT: CompilerConfigurationKey<File> = CompilerConfigurationKey.create("repository root")
    public val CODEOWNERS_FILE: CompilerConfigurationKey<File> = CompilerConfigurationKey.create(".CODEOWNERS file")
    public val MAPPINGS_OUTPUT: CompilerConfigurationKey<File> =
        CompilerConfigurationKey.create(".CODEOWNERS mappings output file")
}
