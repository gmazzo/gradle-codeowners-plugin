package io.github.gmazzo.codeowners.compiler

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

public class CodeOwnersFirExtensionRegistrar(
    private val mappings: CodeOwnersMappings,
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        +{ session: FirSession -> CodeOwnersFirProcessor(session, mappings) }
    }

}
