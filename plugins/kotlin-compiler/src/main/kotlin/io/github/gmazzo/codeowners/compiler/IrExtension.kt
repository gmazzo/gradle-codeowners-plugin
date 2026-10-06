package io.github.gmazzo.codeowners.compiler

import java.io.File
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal class IrExtension(
    private val mappings: Mappings,
    private val mappingsFile: File?,
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val transformer = IrTransformer(pluginContext, mappings)

        moduleFragment.accept(transformer, null)
        mappingsFile?.let(mappings::export)
    }

}
