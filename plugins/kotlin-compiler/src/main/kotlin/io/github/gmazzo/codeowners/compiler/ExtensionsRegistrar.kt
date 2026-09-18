@file:OptIn(ExperimentalCompilerApi::class)

package io.github.gmazzo.codeowners.compiler

import io.github.gmazzo.codeowners.compiler.BuildConfig.COMPILER_PLUGIN_ID
import io.github.gmazzo.codeowners.compiler.ConfigurationKeys.CODEOWNERS_FILE
import io.github.gmazzo.codeowners.compiler.ConfigurationKeys.CODEOWNERS_ROOT
import io.github.gmazzo.codeowners.compiler.ConfigurationKeys.MAPPINGS_OUTPUT
import io.github.gmazzo.codeowners.matcher.CodeOwnersFile
import io.github.gmazzo.codeowners.matcher.CodeOwnersMatcher
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

internal class ExtensionsRegistrar : CompilerPluginRegistrar() {

    override val pluginId: String = COMPILER_PLUGIN_ID

    override val supportsK2 = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (KotlinVersion.CURRENT.major != BuildConfig.EXPECTED_KOTLIN_VERSION.substringBefore('.').toInt()) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "The '$COMPILER_PLUGIN_ID' plugin was designed for Kotlin ${BuildConfig.EXPECTED_KOTLIN_VERSION}, but you are using ${KotlinVersion.CURRENT}"
            )
        }

        val codeOwnersRoot = configuration[CODEOWNERS_ROOT]!!
        val codeOwnersFile = configuration[CODEOWNERS_FILE]!!.useLines { CodeOwnersFile(it) }
        val mappingFile = configuration[MAPPINGS_OUTPUT]
        val matcher = CodeOwnersMatcher(codeOwnersRoot, codeOwnersFile)
        val mappings = Mappings(matcher, mappingFile)

        FirExtensionRegistrarAdapter.registerExtension(FirRegistrar(mappings))
        IrGenerationExtension.registerExtension(IrExtension(mappings))
    }

}
