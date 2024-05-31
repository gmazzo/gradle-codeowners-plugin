package io.github.gmazzo.codeowners.compiler

import io.github.gmazzo.codeowners.matcher.CodeOwnersFile
import io.github.gmazzo.codeowners.matcher.CodeOwnersMatcher
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.io.File
import java.util.LinkedList
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

internal class CodeOwnersIrGenerationExtension(
    private val matcher: CodeOwnersMatcher,
    private val mappingsFile: File?,
) : IrGenerationExtension {

    private val mappings = mappingsFile?.let { mutableMapOf<String, Pair<Set<String>, File>>() }

    private val executor by lazy { Executors.newSingleThreadScheduledExecutor() }

    private var dumpTask: Future<*>? = null

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val transformer = CodeOwnersIrTransformer(pluginContext, matcher, mappings)

        moduleFragment.accept(transformer, InvalidOwners)

        if (mappings != null) {
            postDumpMappings()
        }
    }

    private fun postDumpMappings() {
        dumpTask?.cancel(true)
        dumpTask = executor.schedule(::dumpMappings, 100, TimeUnit.MILLISECONDS)
    }

    private fun dumpMappings() {
        val entries = LinkedList<CodeOwnersFile.Entry>()

        val it = mappings!!.iterator()
        while (it.hasNext()) {
            val (jvmClass, entry) = it.next()
            val (owners, sourceFile) = entry

            if (!sourceFile.isFile) {
                it.remove()

            } else {
                entries.add(CodeOwnersFile.Entry(jvmClass, owners.toList()))
            }
        }

        with(mappingsFile!!) {
            parentFile?.mkdirs()
            writeText(CodeOwnersFile(entries).content)
        }
    }

    private data object InvalidOwners : Set<String> by emptySet() {
        override val size: Int get() = error("Invalid owners")
    }

}
