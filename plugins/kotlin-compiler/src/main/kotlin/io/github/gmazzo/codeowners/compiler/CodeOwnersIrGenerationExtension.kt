package io.github.gmazzo.codeowners.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal class CodeOwnersIrGenerationExtension(
    private val mappings: CodeOwnersMappings,
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        mappings.noteFrontedFinished()

        val transformer = CodeOwnersIrTransformer(pluginContext, mappings)

        moduleFragment.accept(transformer, InvalidOwners)
    }

    @Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
    private data object InvalidOwners : Set<String> by emptySet() {
        override val size: Int get() = error("Invalid owners")
    }

}
