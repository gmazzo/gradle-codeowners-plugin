@file:OptIn(ExperimentalCompilerApi::class)

package io.github.gmazzo.codeowners.compiler

import io.github.gmazzo.codeowners.compiler.CodeOwnersConfigurationKeys.CODEOWNERS_FILE
import io.github.gmazzo.codeowners.compiler.CodeOwnersConfigurationKeys.CODEOWNERS_ROOT
import io.github.gmazzo.codeowners.compiler.CodeOwnersConfigurationKeys.MAPPINGS_OUTPUT
import io.github.gmazzo.codeowners.matcher.CodeOwnersFile
import io.github.gmazzo.codeowners.matcher.CodeOwnersMatcher
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

internal class CodeOwnersComponentRegistrar : CompilerPluginRegistrar() {

    override val supportsK2 = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val codeOwnersRoot = configuration.get(CODEOWNERS_ROOT)!!
        val codeOwnersFile = configuration.get(CODEOWNERS_FILE)!!.useLines { CodeOwnersFile(it) }
        val mappingFile = configuration.get(MAPPINGS_OUTPUT)
        val matcher = CodeOwnersMatcher(codeOwnersRoot, codeOwnersFile)

        mappingFile?.delete()
        mappingFile?.parentFile?.mkdirs()
        IrGenerationExtension.registerExtension(CodeOwnersIrGenerationExtension(matcher, mappingFile))
    }

}
