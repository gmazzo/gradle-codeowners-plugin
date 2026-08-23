package io.github.gmazzo.codeowners.compiler

import io.github.gmazzo.codeowners.compiler.Diagnostics.ILLEGAL_CODEOWNERS_USAGE
import io.github.gmazzo.codeowners.compiler.IrTransformer.Companion.CODEOWNERS_ANNOTATION
import java.io.File
import org.jetbrains.kotlin.backend.common.serialization.toIoFileOrNull
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.java.findJvmNameValue
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.getContainingFile
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

internal class FirChecker(
    session: FirSession,
    private val mappings: Mappings,
) : FirAdditionalCheckersExtension(session) {

    private val cache: FirCache<File, Mappings.Mapping?, Nothing?> =
        session.firCachesFactory.createCache(mappings::resolve)

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val fileCheckers = setOf(FileChecker())
        override val regularClassCheckers = setOf(RegularClassChecker())
        override val simpleFunctionCheckers = setOf(SimpleFunctionChecker())
    }

    abstract inner class Checker<Declaration : FirDeclaration> : FirDeclarationChecker<Declaration>(MppCheckerKind.Common) {

        abstract val Declaration.className: JvmClassName?

        context(context: CheckerContext, reporter: DiagnosticReporter)
        final override fun check(declaration: Declaration) {
            if (declaration.hasAnnotation(CODEOWNERS_ANNOTATION, session)) {
                reporter.reportOn(declaration.source, ILLEGAL_CODEOWNERS_USAGE, context)
                return
            }

            val file = declaration.file?.sourceFile?.toIoFileOrNull() ?: return
            val className = declaration.className ?: return
            cache.getValue(file)?.classes?.add(className.internalName)
        }

        val FirDeclaration.file
            get() = session.firProvider.getContainingFile(symbol)

    }

    inner class FileChecker : Checker<FirFile>() {

        override val FirFile.className: JvmClassName?
            get() = null

    }

    inner class RegularClassChecker : Checker<FirRegularClass>() {

        override val FirRegularClass.className: JvmClassName
            get() = JvmClassName.byClassId(classId)

    }

    inner class SimpleFunctionChecker : Checker<FirNamedFunction>() {

        private val addedFiles = mutableSetOf<FirFile>()

        override val FirNamedFunction.className: JvmClassName?
            get() {
                if (symbol.callableId.classId != null) return null
                val file = file?.takeIf(addedFiles::add) ?: return null

                val fileJvmName = findJvmNameValue() ?: file.name.replace("\\.kt".toRegex(), "Kt")
                val fileClassName = file.packageFqName.child(Name.identifier(fileJvmName))

                return JvmClassName.byFqNameWithoutInnerClasses(fileClassName)
            }

    }

}
