package io.github.gmazzo.codeowners.compiler

import org.jetbrains.kotlin.backend.common.serialization.toIoFileOrNull
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFileChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.java.findJvmNameValue
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

public class CodeOwnersFirProcessor(
    session: FirSession,
    private val mappings: CodeOwnersMappings,
) : FirAdditionalCheckersExtension(session) {

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val fileCheckers = setOf(CodeOwnersMapper())
    }

    public inner class CodeOwnersMapper : FirFileChecker(MppCheckerKind.Platform) {
        context(_: CheckerContext, _: DiagnosticReporter)
        override fun check(declaration: FirFile) {
            val mappings = declaration.sourceFile?.toIoFileOrNull()?.let(mappings::resolve) ?: return

            declaration.accept(fileVisitor, mappings)
        }
    }

    @OptIn(DirectDeclarationsAccess::class)
    private val fileVisitor = object : FirVisitor<Unit, CodeOwnersMappings.Mapping>() {

        override fun visitFile(file: FirFile, data: CodeOwnersMappings.Mapping) {
            file.acceptChildren(this, data)

            if (file.declarations.any { it is FirSimpleFunction }) {
                val fileJvmName = file.findJvmNameValue() ?: file.name.replace("\\.kt".toRegex(), "Kt")
                val fileClass =
                    JvmClassName.byFqNameWithoutInnerClasses(file.packageFqName.child(Name.identifier(fileJvmName))).internalName

                data.classes += fileClass
            }
        }

        override fun visitRegularClass(regularClass: FirRegularClass, data: CodeOwnersMappings.Mapping) {
            data.classes += JvmClassName.byClassId(regularClass.classId).internalName

            regularClass.acceptChildren(this, data)
        }

        override fun visitElement(element: FirElement, data: CodeOwnersMappings.Mapping) {
        }

    }

}
