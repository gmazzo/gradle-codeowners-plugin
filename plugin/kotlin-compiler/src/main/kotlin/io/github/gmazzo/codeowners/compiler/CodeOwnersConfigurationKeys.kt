package io.github.gmazzo.codeowners.compiler

import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.io.File

object CodeOwnersConfigurationKeys {
    val CODEOWNERS_ROOT: CompilerConfigurationKey<File> = CompilerConfigurationKey.create("repository root")
    val CODEOWNERS_FILE: CompilerConfigurationKey<File> = CompilerConfigurationKey.create(".CODEOWNERS file")
}
