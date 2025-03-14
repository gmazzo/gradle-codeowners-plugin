package io.github.gmazzo.codeowners.compiler

import java.io.File
import org.jetbrains.kotlin.config.CompilerConfigurationKey

object CodeOwnersConfigurationKeys {
    val CODEOWNERS_ROOT: CompilerConfigurationKey<File> = CompilerConfigurationKey.create("repository root")
    val CODEOWNERS_FILE: CompilerConfigurationKey<File> = CompilerConfigurationKey.create(".CODEOWNERS file")
    val MAPPINGS_OUTPUT: CompilerConfigurationKey<File> =
        CompilerConfigurationKey.create(".CODEOWNERS mappings output file")
}
