package io.github.gmazzo.codeowners.compiler

import org.jetbrains.kotlin.backend.common.extensions.DeclarationFinder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.getIoFile
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.backend.js.utils.nameWithoutExtension
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrAnnotationImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.konan.isNative

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class IrTransformer(
    private val context: IrPluginContext,
    private val mappings: Mappings,
) : IrElementTransformerVoid() {

    private val requiresProvider = context.platform?.let { it.isJs() || it.isNative() } == true

    private val stringArray =
        context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.stringType)

    private var lastForOwners: Set<String> = emptySet()
    private lateinit var lastAnnotation: IrAnnotation
    private lateinit var lastAnnotationProvider: Lazy<IrAnnotation>

    override fun visitFile(declaration: IrFile) = declaration.apply {
        val owners = declaration.getIoFile()?.let(mappings::resolve)?.owners ?: return@apply

        ensureAnnotation(declaration, owners)

        annotations += lastAnnotation
        super.visitFile(declaration)
    }

    override fun visitClass(declaration: IrClass) = declaration.apply {
        // we only decorate top level classes
        if (parent !is IrFile) return@apply

        annotations += lastAnnotation
        if (requiresProvider) {
            annotations += lastAnnotationProvider.value
        }

        super.visitClass(declaration)
    }

    private fun ensureAnnotation(file: IrFile, forOwners: Set<String>) {
        if (lastForOwners != forOwners) {
            lastForOwners = forOwners

            val ownersValue = IrVarargImpl(
                SYNTHETIC_OFFSET,
                SYNTHETIC_OFFSET,
                stringArray,
                context.irBuiltIns.stringType,
                forOwners.map { value ->
                    IrConstImpl.string(
                        SYNTHETIC_OFFSET,
                        SYNTHETIC_OFFSET,
                        context.irBuiltIns.stringType,
                        value,
                    )
                },
            )

            val finder = context.finderForSource(file)

            val annotationSymbol = finder.resolve(CODEOWNERS_ANNOTATION)
            lastAnnotation = IrAnnotationImpl.fromSymbolOwner(
                SYNTHETIC_OFFSET,
                SYNTHETIC_OFFSET,
                annotationSymbol.defaultType,
                annotationSymbol.owner.primaryConstructor!!.symbol,
            ).apply { arguments[0] = ownersValue }

            val providerClass by lazy {
                createProvider($$"$${file.nameWithoutExtension}$CODEOWNERS", finder, ownersValue)
                    .also(file::addChild)
                    .symbol
            }

            lastAnnotationProvider = lazy {
                val providerKeySymbol = finder.resolve(CODEOWNERS_PROVIDER_KEY_ANNOTATION)
                val starType = providerClass.starProjectedType

                IrAnnotationImpl.fromSymbolOwner(
                    SYNTHETIC_OFFSET,
                    SYNTHETIC_OFFSET,
                    providerKeySymbol.defaultType,
                    providerKeySymbol.owner.primaryConstructor!!.symbol,
                ).apply {
                    arguments[0] = IrClassReferenceImpl(
                        SYNTHETIC_OFFSET,
                        SYNTHETIC_OFFSET,
                        context.irBuiltIns.kClassClass.starProjectedType,
                        starType.classifierOrFail,
                        starType,
                    )
                }
            }
        }
    }

    private fun createProvider(className: String, finder: DeclarationFinder, owners: IrVararg) = context.irFactory.buildClass {
        startOffset = SYNTHETIC_OFFSET
        endOffset = SYNTHETIC_OFFSET
        name = Name.identifier(className)
        kind = ClassKind.OBJECT
        visibility = DescriptorVisibilities.INTERNAL
    }.apply {
        val providerSymbol = finder.resolve(CODEOWNERS_PROVIDER_CLASS)

        superTypes += providerSymbol.defaultType
        createThisReceiverParameter()
        addConstructor {
            isPrimary = true
            visibility = DescriptorVisibilities.INTERNAL
        }.body = context.irFactory.createBlockBody(
            SYNTHETIC_OFFSET,
            SYNTHETIC_OFFSET,
            listOfNotNull(
                IrDelegatingConstructorCallImpl.fromSymbolOwner(
                    SYNTHETIC_OFFSET,
                    SYNTHETIC_OFFSET,
                    context.irBuiltIns.unitType,
                    providerSymbol.owner.primaryConstructor!!.symbol
                ).apply { arguments[0] = owners },
                IrInstanceInitializerCallImpl(
                    SYNTHETIC_OFFSET,
                    SYNTHETIC_OFFSET,
                    context.irBuiltIns.anyClass,
                    context.irBuiltIns.unitType
                )
            )
        )
    }

    companion object {

        internal val CODEOWNERS_ANNOTATION =
            ClassId.topLevel(FqName("io.github.gmazzo.codeowners.CodeOwners"))

        private val CODEOWNERS_PROVIDER_CLASS =
            ClassId.topLevel(FqName("io.github.gmazzo.codeowners.CodeOwnersProvider"))

        private val CODEOWNERS_PROVIDER_KEY_ANNOTATION =
            ClassId.topLevel(FqName("io.github.gmazzo.codeowners.CodeOwnersProviderKey"))

        private fun DeclarationFinder.resolve(classId: ClassId) =
            checkNotNull(findClass(classId)) { "Class $classId not found" }

    }

}
