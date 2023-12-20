@file:OptIn(ExperimentalCompilerApi::class)

package io.github.gmazzo.codeowners.compiler

import io.github.gmazzo.codeowners.compiler.BuildConfig.ARG_CODEOWNERS_FILE
import io.github.gmazzo.codeowners.compiler.BuildConfig.ARG_CODEOWNERS_ROOT
import io.github.gmazzo.codeowners.compiler.BuildConfig.COMPILER_PLUGIN_ID
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

internal class CodeOwnersCommandLineProcessor : CommandLineProcessor {

    override val pluginId = COMPILER_PLUGIN_ID

    override val pluginOptions = listOf(CODEOWNERS_ROOT, CODEOWNERS_FILE)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) = when (option) {
        CODEOWNERS_ROOT -> configuration.put(CodeOwnersConfigurationKeys.CODEOWNERS_ROOT, File(value))
        CODEOWNERS_FILE -> configuration.put(CodeOwnersConfigurationKeys.CODEOWNERS_FILE, File(value))
        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }

    companion object {

        val CODEOWNERS_ROOT = CliOption(
            ARG_CODEOWNERS_ROOT, "<path>", "Path to the root of the repository",
            required = true, allowMultipleOccurrences = false
        )

        val CODEOWNERS_FILE = CliOption(
            ARG_CODEOWNERS_FILE, "<path>", "Path to the CODEOWNERS file",
            required = true, allowMultipleOccurrences = false
        )

    }

}
