package io.github.gmazzo.codeowners.compiler

import io.github.gmazzo.codeowners.matcher.CodeOwnersMatcher
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.io.File

internal class CodeOwnersIrGenerationExtension(
    private val matcher: CodeOwnersMatcher,
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val transformer = CodeOwnersIrTransformer(pluginContext)
        val owners = moduleFragment.files.asSequence()
            .flatMap { matcher.ownerOf(File(it.fileEntry.name)).orEmpty() }
            .toSet()

        if (owners.isNotEmpty()) {
            moduleFragment.accept(transformer, owners)
        }
    }

}
