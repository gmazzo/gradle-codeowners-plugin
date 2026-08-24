package io.github.gmazzo.codeowners.compiler

import io.github.gmazzo.codeowners.compiler.IrTransformer.Companion.CODEOWNERS_ANNOTATION
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

internal object Diagnostics : KtDiagnosticsContainer() {

    val ILLEGAL_CODEOWNERS_USAGE: KtDiagnosticFactory0 = KtDiagnosticFactory0(
        name = "ILLEGAL_CODEOWNERS_USAGE",
        severity = Severity.ERROR,
        defaultPositioningStrategy = SourceElementPositioningStrategies.DEFAULT,
        psiType = FirBasedSymbol::class,
        rendererFactory = getRendererFactory(),
    )

    override fun getRendererFactory() = object : BaseDiagnosticRendererFactory() {

        override val MAP by KtDiagnosticFactoryToRendererMap("CodeOwnersPlugin") {
            it.put(
                ILLEGAL_CODEOWNERS_USAGE,
                "@${CODEOWNERS_ANNOTATION.shortClassName} can not be used in code"
            )
        }

    }

}
