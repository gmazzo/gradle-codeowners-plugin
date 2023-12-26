@file:OptIn(ExperimentalCompilerApi::class)

package io.github.gmazzo.codeowners.compiler

import io.github.gmazzo.codeowners.compiler.BuildConfig.COMPILER_PLUGIN_ID
import io.github.gmazzo.codeowners.compiler.CodeOwnersConfigurationKeys.CODEOWNERS_FILE
import io.github.gmazzo.codeowners.compiler.CodeOwnersConfigurationKeys.CODEOWNERS_ROOT
import io.github.gmazzo.codeowners.compiler.CodeOwnersConfigurationKeys.MAPPINGS_OUTPUT
import io.github.gmazzo.codeowners.matcher.CodeOwnersFile
import io.github.gmazzo.codeowners.matcher.CodeOwnersMatcher
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.resolverLogger

internal class CodeOwnersComponentRegistrar : CompilerPluginRegistrar() {

    override val supportsK2 = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (KotlinVersion.CURRENT.toString() != BuildConfig.EXPECTED_KOTLIN_VERSION) {
            configuration.resolverLogger.warning("The '$COMPILER_PLUGIN_ID' plugin was designed for Kotlin " +
                    "${BuildConfig.EXPECTED_KOTLIN_VERSION}, but you are using ${KotlinVersion.CURRENT}")
        }

        val codeOwnersRoot = configuration.get(CODEOWNERS_ROOT)!!
        val codeOwnersFile = configuration.get(CODEOWNERS_FILE)!!.useLines { CodeOwnersFile(it) }
        val mappingFile = configuration.get(MAPPINGS_OUTPUT)
        val matcher = CodeOwnersMatcher(codeOwnersRoot, codeOwnersFile)

        mappingFile?.delete()
        mappingFile?.parentFile?.mkdirs()
        IrGenerationExtension.registerExtension(CodeOwnersIrGenerationExtension(matcher, mappingFile))
    }

}
