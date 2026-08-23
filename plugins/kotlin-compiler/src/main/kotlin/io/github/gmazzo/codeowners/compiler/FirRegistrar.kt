package io.github.gmazzo.codeowners.compiler

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

internal class FirRegistrar(
    private val mappings: Mappings,
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        +{ session: FirSession -> FirChecker(session, mappings) }
    }

}
