package io.github.gmazzo.codeowners.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal class IrExtension(
    private val mappings: Mappings,
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        mappings.noteFrontedFinished()

        val transformer = IrTransformer(pluginContext, mappings)

        moduleFragment.accept(transformer, null)
    }

}
