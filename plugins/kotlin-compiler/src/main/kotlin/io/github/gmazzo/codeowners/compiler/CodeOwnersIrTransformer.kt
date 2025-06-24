@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.gmazzo.codeowners.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.getIoFile
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.utils.nameWithoutExtension
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrMutableAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.konan.isNative

internal class CodeOwnersIrTransformer(
    private val context: IrPluginContext,
    private val mappings: CodeOwnersMappings,
) : IrTransformer<Set<String>>() {

    private val requiresProvider = context.platform?.let { it.isJs() || it.isNative() } == true

    private val stringArray = context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.stringType)

    private val annotationCodeOwners by lazy {
        context.referenceClass(ClassId.fromString("io/github/gmazzo/codeowners/CodeOwners"))!!
    }

    private val annotationCodeOwnersProvider by lazy {
        context.referenceClass(ClassId.fromString("io/github/gmazzo/codeowners/CodeOwnersProviderKey"))!!
    }

    private val classCodeOwnersProvider by lazy {
        context.referenceClass(ClassId.fromString("io/github/gmazzo/codeowners/CodeOwnersProvider"))!!
    }

    private var fileCodeOwnersProvider: IrClassSymbol? = null

    override fun visitFile(declaration: IrFile, data: Set<String>) = declaration.apply {
        val owners = declaration.getIoFile()?.let(mappings::resolve)?.owners ?: return@apply

        addAnnotation(owners)
        super.visitFile(declaration, owners)

        fileCodeOwnersProvider = null
    }

    override fun visitClass(declaration: IrClass, data: Set<String>) = declaration.apply {
        // we only decorate top level classes
        if (parent !is IrFile) return@apply

        val ownersValue = addAnnotation(data)

        if (requiresProvider) {
            addCodeOwnersProviderAnnotation(ownersValue)
        }

        super.visitClass(declaration, data)
    }

    private fun IrMutableAnnotationContainer.addAnnotation(owners: Set<String>): IrVararg {
        val ownersValue = IrVarargImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            stringArray,
            context.irBuiltIns.stringType,
            owners.map { value ->
                IrConstImpl.string(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.stringType,
                    value,
                )
            },
        )

        annotations += IrConstructorCallImpl.fromSymbolOwner(
            SYNTHETIC_OFFSET,
            SYNTHETIC_OFFSET,
            annotationCodeOwners.defaultType,
            annotationCodeOwners.owner.primaryConstructor!!.symbol,
        ).apply { arguments[0] = ownersValue }
        return ownersValue
    }

    private fun IrFile.addCodeOwnersProvider(className: String, owners: IrVararg) = context.irFactory.buildClass {
        startOffset = SYNTHETIC_OFFSET
        endOffset = SYNTHETIC_OFFSET
        name = Name.identifier(className)
        kind = ClassKind.OBJECT
        visibility = DescriptorVisibilities.INTERNAL
    }.apply {
        this@addCodeOwnersProvider.addChild(this)

        superTypes += classCodeOwnersProvider.defaultType
        createThisReceiverParameter()
        addConstructor {
            isPrimary = true
            visibility = DescriptorVisibilities.INTERNAL
        }.body = context.irFactory.createBlockBody(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            listOfNotNull(
                IrDelegatingConstructorCallImpl.fromSymbolOwner(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.unitType,
                    classCodeOwnersProvider.owner.primaryConstructor!!.symbol
                ).apply { arguments[0] = owners },
                IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.anyClass,
                    context.irBuiltIns.unitType
                )
            )
        )
    }.symbol

    private fun IrClass.addCodeOwnersProviderAnnotation(ownersValue: IrVararg) {
        if (fileCodeOwnersProvider == null) {
            val file = fileOrNull ?: return

            fileCodeOwnersProvider = file.addCodeOwnersProvider("${file.nameWithoutExtension}\$CODEOWNERS", ownersValue)
        }

        annotations += IrConstructorCallImpl.fromSymbolOwner(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            annotationCodeOwnersProvider.defaultType,
            annotationCodeOwnersProvider.owner.primaryConstructor!!.symbol,
        ).apply {
            val starType = fileCodeOwnersProvider!!.starProjectedType

            arguments[0] = IrClassReferenceImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                context.irBuiltIns.kClassClass.starProjectedType,
                starType.classifierOrFail,
                starType,
            )
        }
    }

}
