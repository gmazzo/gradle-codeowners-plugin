package io.github.gmazzo.codeowners.compiler

import io.github.gmazzo.codeowners.matcher.CodeOwnersFile
import io.github.gmazzo.codeowners.matcher.CodeOwnersMatcher
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.io.File

internal class CodeOwnersIrGenerationExtension(
    private val matcher: CodeOwnersMatcher,
    private val mappingsFile: File?,
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val mappings = mappingsFile?.let { mutableMapOf<String, MutableSet<String>>() }
        val transformer = CodeOwnersIrTransformer(pluginContext, matcher, mappings)

        moduleFragment.accept(transformer, InvalidOwners)

        if (mappings != null) {
            val entries = mappings.map { (file, owners) -> CodeOwnersFile.Entry(file, owners.toList()) }
            val codeOwners = CodeOwnersFile(entries)

            mappingsFile!!.appendText(codeOwners.content)
        }
    }

    private data object InvalidOwners : Set<String> by emptySet() {
        override val size: Int get() = error("Invalid owners")
    }

}
