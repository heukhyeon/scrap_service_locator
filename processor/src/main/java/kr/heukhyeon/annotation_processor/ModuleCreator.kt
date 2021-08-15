package kr.heukhyeon.annotation_processor

import com.squareup.kotlinpoet.*
import kr.heukhyeon.service_locator.Component
import kr.heukhyeon.service_locator.ComponentOwner
import kr.heukhyeon.service_locator.EntryPoint
import kr.heukhyeon.service_locator.provider.Provider
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror


open class ModuleCreator(
    protected val classSpec: TypeSpec.Builder,
    process: ProcessingEnvironment,
    env: RoundEnvironment
) : ClassCreator(process) {
    private val targets = env.getElementsAnnotatedWith(Component::class.java)


    private val componentTypes = targets.map { it.asType() }.toSet()
    private val additionalImplementTypes =  env
        .getElementsAnnotatedWith(EntryPoint::class.java)
        .flatMap { entryPoint ->
        entryPoint.enclosedElements.mapNotNull<Element, TypeMirror> { element ->
            if (element.asType().toString() == Provider::class.qualifiedName) {
                val realName =
                    element.simpleName.substring(0, element.simpleName.indexOf("\$delegate"))
                val functionName = "get" + realName.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }

                return@mapNotNull (entryPoint.enclosedElements.first { it.simpleName.toString() == functionName } as ExecutableElement)
                    .returnType
            }
            return@mapNotNull null
        }
    }
        .subtract(componentTypes)
        .toMutableSet()

    private val parcelableTypes = getType("android.os.Parcelable")
    private val viewBindingTypes = getType("androidx.viewbinding.ViewBinding")
    private val parentListenerTypes = getType("kr.heukhyeon.service_locator.FragmentParentListener")

    private val bindingProviderType =
        getType("kr.heukhyeon.service_locator.initializer.provider.ViewBindingProvider")

    private var containParcelable = false
    private var containParentListener = false
    private var containViewBindingProvider = false

    fun create() {
        println("시작은 됨")
        targets.forEach {
            val componentType = getComponentType(it)
            val isSingleton = it.getAnnotation(Component::class.java).isSingleton
            generateGetterFunction(componentType, generateFactoryCodeForNormal(it, componentType), isSingleton)
        }
        additionalImplementTypes
            .sortedBy {
                when {
                    it.isSubType(viewBindingTypes) -> 1
                    it.isSubType(parentListenerTypes) -> 2
                    it.isSubType(parcelableTypes) -> 3
                    else -> 4
                }
            }
            .forEach(this::generateAdditionalParamsFunction)

        if (containParcelable) generateAbstractGetterParcelableFunction()
        if (containParentListener) generateAbstractGetterParentListenerFunction()
        if (containViewBindingProvider) generateAbstractGetterViewBindingProviderFunction()

    }

    private fun generateGetterFunction(componentType: ClassName, factory: CodeBlock, isSingleton: Boolean) {
        val owner = if (isSingleton) "IComponentModule.SINGLETON_OWNER" else "owner"

        FunSpec.builder("get" + componentType.realName(false))
            .addModifiers(KModifier.SUSPEND)
            .addParameter("owner", ComponentOwner::class)
            .addCode("return cachingAndReturn(\n")
            .addCodeWithTab(2, "owner = ${owner},\n")
            .addCodeWithTab(2, "keyClazz = %T::class,\n", componentType)
            .addCodeWithTab(2, "factory = ")
            .addCode(factory)
            .addCodeWithTab(1, ")")
            .returns(componentType)
            .build()
            .also(classSpec::addFunction)

    }

    private fun generateAdditionalParamsFunction(typeMirror: TypeMirror) {
        val componentType = typeMirror.toClassName()
        when {
            typeMirror.isSubType(parcelableTypes) -> {
                containParcelable = true
                generateGetterFunction(componentType, generateFactoryCodeForParcelable(), false)
            }
            typeMirror.isSubType(parentListenerTypes) -> {
                containParentListener = true
                generateGetterFunction(componentType, generateFactoryCodeForParentListener(), false)
            }
            typeMirror.isSubType(viewBindingTypes) -> {
                containViewBindingProvider = true
                generateGetterFunction(
                    componentType, generateFactoryCodeForViewBinding(
                        componentType
                    ), false)
            }
            else -> FunSpec.builder("get" + componentType.realName(false))
                .addModifiers(KModifier.SUSPEND)
                .addParameter("owner", ComponentOwner::class)
                .returns(componentType)
                .addModifiers(KModifier.ABSTRACT)
                .build()
                .also(classSpec::addFunction)
        }
    }

    protected fun getComponentType(element: Element): ClassName {
        val clazz = try {
            element.getAnnotation(Component::class.java).bind.qualifiedName
        } catch (e: MirroredTypeException) {
            e.typeMirror.toString();
        }
        return if (clazz == "kotlin.Any" || clazz == "java.lang.Object") element.toClassName()
        else ClassName.bestGuess(clazz!!)
    }

    protected fun generateFactoryCodeForNormal(element: Element, targetType: ClassName): CodeBlock {
        val constructor =
            element.enclosedElements.first { it.kind == ElementKind.CONSTRUCTOR } as ExecutableElement

        constructor.parameters.filter { componentTypes.contains(it.asType()).not() }.forEach {
            additionalImplementTypes.add(it.asType())
        }

        val params = constructor.parameters
            .map {
                it.simpleName.toString() to it.asType().toClassName().realName(false)
            }

        return CodeBlock.builder()
            .add("{\n")
            .addCodeWithTab(3, "%T(\n", element.asType())
            .also { block ->
                params.forEach { (name, clazz) ->
                    block.addCodeWithTab(4, "$name = get$clazz(owner),\n")
                }
            }
            .addCodeWithTab(3, ")\n")
            .addCodeWithTab(2, "}\n")
            .build()
    }

    protected fun generateFactoryCodeForViewBinding(targetType: ClassName): CodeBlock {
        return CodeBlock.builder()
            .add("{\n")
            .addCodeWithTab(3, "getViewBindingProvider(owner).create(\n")
            .addCodeWithTab(4, "layoutId = ${viewBindingToPackageAndLayoutId(targetType)},\n")
            .addCodeWithTab(4, "factory = ${targetType.simpleName}::bind\n")
            .addCodeWithTab(3, ")\n")
            .addCodeWithTab(2, "}\n")
            .build()
    }

    protected fun generateFactoryCodeForParcelable(): CodeBlock {
        return CodeBlock.builder()
            .add("{\n")
            .addCodeWithTab(3, "getParcelable(owner)\n")
            .addCodeWithTab(2, "}\n")
            .build()
    }

    protected fun generateFactoryCodeForParentListener(): CodeBlock {
        return CodeBlock.builder()
            .add("{\n")
            .addCodeWithTab(3, "getFragmentParentListener(owner)\n")
            .addCodeWithTab(2, "}\n")
            .build()
    }


    protected fun generateAbstractGetterParcelableFunction() {
        val bounds = parcelableTypes!!.toClassName()
        FunSpec.builder("getParcelable")
            .addModifiers(KModifier.SUSPEND)
            .addModifiers(KModifier.ABSTRACT)
            .addTypeVariable(TypeVariableName.Companion.invoke("T", bounds))
            .addParameter("owner", ComponentOwner::class)
            .returns(TypeVariableName.invoke("T"))
            .build()
            .also(classSpec::addFunction)
    }

    protected fun generateAbstractGetterParentListenerFunction() {
        val bounds = parentListenerTypes!!.toClassName()
        FunSpec.builder("getFragmentParentListener")
            .addModifiers(KModifier.SUSPEND)
            .addModifiers(KModifier.ABSTRACT)
            .addTypeVariable(TypeVariableName.Companion.invoke("T", bounds))
            .addParameter("owner", ComponentOwner::class)
            .returns(TypeVariableName.invoke("T"))
            .build()
            .also(classSpec::addFunction)
    }

    protected fun generateAbstractGetterViewBindingProviderFunction() {
        FunSpec.builder("getViewBindingProvider")
            .addModifiers(KModifier.SUSPEND)
            .addModifiers(KModifier.ABSTRACT)
            .addParameter("owner", ComponentOwner::class)
            .returns(bindingProviderType!!.toClassName())
            .build()
            .also(classSpec::addFunction)
    }
}