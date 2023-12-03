package io.github.gmazzo.codeowners.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.utils.nameWithoutExtension
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrMutableAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.konan.isNative

internal class CodeOwnersIrTransformer(
    private val context: IrPluginContext,
) : IrElementTransformer<Set<String>> {

    private val isJS = context.platform?.isJs() == true

    private val isNative = context.platform?.isNative() == true

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

    private val fileCodeOwnersProviders = mutableMapOf<Set<String>, IrClassSymbol>()

    override fun visitFile(declaration: IrFile, data: Set<String>) = declaration.apply {
        addAnnotation(data)

        super.visitFile(declaration, data)
        fileCodeOwnersProviders.clear()
    }

    override fun visitClass(declaration: IrClass, data: Set<String>) = declaration.apply {
        val ownersValue = addAnnotation(data)

        if (isJS || isNative) {
            fileOrNull?.let { file ->
                val provider = file.getOrCreateCodeOwnersProvider(data, ownersValue)

                addCodeOwnersProviderAnnotation(provider)
            }
        }

        super.visitClass(declaration, data)
    }

    private fun IrMutableAnnotationContainer.addAnnotation(owners: Set<String>): IrVararg {
        val exisingOwners = findOwnersFromExistingAnnotation()
        val ownersValue = IrVarargImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            stringArray,
            context.irBuiltIns.stringType,
            (exisingOwners ?: owners).map { value ->
                IrConstImpl.string(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.stringType,
                    value,
                )
            },
        )

        if (exisingOwners == null) {
            annotations += IrConstructorCallImpl.fromSymbolOwner(
                SYNTHETIC_OFFSET,
                SYNTHETIC_OFFSET,
                annotationCodeOwners.defaultType,
                annotationCodeOwners.owner.primaryConstructor!!.symbol,
            ).apply {
                putValueArgument(0, ownersValue)
            }
        }
        return ownersValue
    }

    private fun IrAnnotationContainer.findOwnersFromExistingAnnotation(): Set<String>? {
        val annotation = annotations
            .find { it.type.classFqName == annotationCodeOwners.owner.fqNameWhenAvailable } ?: return null

        return (annotation.valueArguments[0] as IrVararg).elements.mapTo(linkedSetOf()) {
            @Suppress("UNCHECKED_CAST")
            (it as IrConst<String>).value
        }
    }

    private fun IrFile.getOrCreateCodeOwnersProvider(
        owners: Set<String>,
        ownersValue: IrVararg,
    ) = fileCodeOwnersProviders.getOrPut(owners) {
        addCodeOwnersProvider("${nameWithoutExtension}\$CODEOWNERS_${fileCodeOwnersProviders.size}", ownersValue)
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
        createImplicitParameterDeclarationWithWrappedDescriptor()
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
                ).apply {
                    putValueArgument(0, owners)
                },
                IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.anyClass,
                    context.irBuiltIns.unitType
                )
            )
        )
    }.symbol

    private fun IrClass.addCodeOwnersProviderAnnotation(provider: IrClassSymbol) {
        annotations += IrConstructorCallImpl.fromSymbolOwner(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            annotationCodeOwnersProvider.defaultType,
            annotationCodeOwnersProvider.owner.primaryConstructor!!.symbol,
        ).apply {
            putValueArgument(
                0, IrClassReferenceImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.kClassClass.starProjectedType,
                    provider.starProjectedType.classifierOrFail,
                    provider.starProjectedType,
                )
            )
        }
    }

}
