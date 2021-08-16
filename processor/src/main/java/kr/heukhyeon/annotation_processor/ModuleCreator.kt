package kr.heukhyeon.annotation_processor

import com.squareup.kotlinpoet.*
import kr.heukhyeon.service_locator.*
import kr.heukhyeon.service_locator.provider.Provider
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror


class ModuleCreator(
    moduleName: String,
    process: ProcessingEnvironment,
    env: RoundEnvironment
) : ClassCreator(process) {
    private val targets = env.getElementsAnnotatedWith(Component::class.java)

    private val classSpec: TypeSpec.Builder = TypeSpec.interfaceBuilder(moduleName)
        .addSuperinterface(IComponentModule::class)
        .addAnnotation(
            AnnotationSpec.builder(ComponentModule::class.java)
                .build()
        )

    private val componentTypes = targets.map { it.asType() }.toSet()

    /**
     * @EntryPoint
     * class SomeActivity : AppCompatActivity {
     *      private val binding by inject(MyViewBinding::class)
     * }
     *
     * ViewBinding 과 같이, @Component 어노테이션이 붙은 컴포넌트에는 요구되지 않지만,
     * @EntryPoint 어노테이션이 붙은 클래스에서 요구되는 경우가 존재할수 있다.
     *
     * 이에 @EntryPoint 어노테이션이 붙은 클래스에서 [ComponentOwner.inject] 함수가 호출된경우,
     * 해당 함수의 종속성 타입을 추가적으로 구현한다.
     */
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

    var generatedTypes = emptyList<TypeName>()

    fun create() : TypeSpec {
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

        /**
         * 현재 작성중인 모듈에서 Parcelable에 대한 종속성이 요구되는경우, Parcelable 추상함수를 추가한다.
         */
        if (containParcelable) generateAbstractGetterParcelableFunction()
        /**
         * 현재 작성중인 모듈에서 FragmentParentListener 에 대한 종속성이 요구되는경우, Parcelable 추상함수를 추가한다.
         */
        if (containParentListener) generateAbstractGetterParentListenerFunction()
        /**
         * 현재 작성중인 모듈에서 ViewBinding 에 대한 종속성이 요구되는경우, ViewBindingProvider 추상함수를 추가한다.
         */
        if (containViewBindingProvider) generateAbstractGetterViewBindingProviderFunction()

        generatedTypes = classSpec.funSpecs
            .filter { it.modifiers.contains(KModifier.ABSTRACT).not() }
            .mapNotNull { it.returnType }
        return classSpec.build()
    }

    /**
     * 객체 주입 요청이 들어왔을때 해당 객체를 반환하는 함수를 추가한다.
     * @params
     * [componentType] : 반환되는 타입
     * [factory] : 해당 객체의 생성방법, 일반적으로 [generateFactoryCodeForNormal] 다.
     * [isSingleton] : [Component.isSingleton] 을 통해 획득한, 해당 객체가 싱글턴 객체로 존재해야하는지 여부
     */
    private fun generateGetterFunction(componentType: ClassName, factory: CodeBlock, isSingleton: Boolean) {
        val owner = if (isSingleton) "IComponentModule.SINGLETON_OWNER" else "owner"

        additionalImplementTypes.removeIf { it.toClassName() == componentType }

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

    /**
     * [Component] 어노테이션이 붙은 객체가 스스로를 다른 타입으로 바인딩한다 지정한경우 ([Component.bind] != Any::class)
     * 해당 타입으로 반환하고, 그렇지않은경우 객체 자신의 타입을 반환한다.
     */
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

    /**
     * ViewBinding 을 상속하는 타입에 대한 객체 생성 함수를 정의한다.
     */
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

    /**
     * Parcelable 을 상속하는 타입에 대한 객체 생성 함수를 정의한다.
     */
    protected fun generateFactoryCodeForParcelable(): CodeBlock {
        return CodeBlock.builder()
            .add("{\n")
            .addCodeWithTab(3, "getParcelable(owner)\n")
            .addCodeWithTab(2, "}\n")
            .build()
    }

    /**
     * FragmentParentListener 를 상속하는 타입에 대한 객체 생성 함수를 정의한다.
     */
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