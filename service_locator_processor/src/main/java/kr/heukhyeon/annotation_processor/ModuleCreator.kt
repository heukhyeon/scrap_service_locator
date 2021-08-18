package kr.heukhyeon.annotation_processor

import com.squareup.kotlinpoet.*
import kr.heukhyeon.service_locator.*
import kr.heukhyeon.service_locator.provider.Provider
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror


class ModuleCreator(
    moduleName: String,
    process: ProcessingEnvironment,
    env: RoundEnvironment
) : CodeCreateHelper(process) {
    private val targets = env.getElementsAnnotatedWith(Component::class.java)
    private val factory = FactoryCodeGenerator(process)
    private val absGenerator = AbstractCodeGenerator(process)
    private val implementMethods = mutableListOf<GetterFunSpecBuilder>()

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
    private val notImplementedMethods = env
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
        .map { GetterFunSpecBuilder.Parameter("", it, null) }
        .toMutableList()

    private val classSpec: TypeSpec.Builder = TypeSpec.interfaceBuilder(moduleName)
        .addSuperinterface(IComponentModule::class)
        .addAnnotation(
            AnnotationSpec.builder(ComponentModule::class.java)
                .build()
        )



    private var containParcelable = false
    private var containParentListener = false
    private var containViewBindingProvider = false

    /**
     * FunctionName to ReturnType
     */
    var generatedTypes = emptyList<Pair<String, TypeName>>()

    fun create(): TypeSpec {
        targets.forEach { it ->
            val componentType = getComponentType(it)
            val isSingleton = it.getAnnotation(Component::class.java).isSingleton
            implementMethods.add(
                GetterFunSpecBuilder(
                    componentType,
                    isSingleton,
                    it.getComponentQualifier(),
                    factory.generateFactoryCodeForNormal(it, notImplementedMethods)
                )
            )
        }

        implementMethods.forEach { it.build(classSpec) }

        notImplementedMethods
            .map {
                createGetterFunctionName(it.typeName, it.qualifier?.annotationType) to it
            }
            .distinctBy { (methodName, _) -> methodName }
            .filter { (methodName, _) -> classSpec.funSpecs.none { it.name == methodName } }
            .map { (_, param) -> param }
            .forEach(this::generateAdditionalParamsFunction)

        implementMethods.forEach { it.build(classSpec) }

        /**
         * 현재 작성중인 모듈에서 Parcelable에 대한 종속성이 요구되는경우, Parcelable 추상함수를 추가한다.
         */
        if (containParcelable) absGenerator.generateAbstractGetterParcelableFunction().also(classSpec::addFunction)
        /**
         * 현재 작성중인 모듈에서 FragmentParentListener 에 대한 종속성이 요구되는경우, Parcelable 추상함수를 추가한다.
         */
        if (containParentListener) absGenerator.generateAbstractGetterParentListenerFunction().also(classSpec::addFunction)
        /**
         * 현재 작성중인 모듈에서 ViewBinding 에 대한 종속성이 요구되는경우, ViewBindingProvider 추상함수를 추가한다.
         */
        if (containViewBindingProvider) absGenerator.generateAbstractGetterViewBindingProviderFunction().also(classSpec::addFunction)

        generatedTypes = classSpec.funSpecs
            .filter { it.modifiers.contains(KModifier.ABSTRACT).not() }
            .mapNotNull {
                val returnType = it.returnType ?: return@mapNotNull null
                it.name to returnType
            }
        return classSpec.build()
    }

    private fun generateAdditionalParamsFunction(param: GetterFunSpecBuilder.Parameter) {
        val typeMirror = param.type
        val componentType = typeMirror.toClassName()
        when {
            typeMirror.isSubType(absGenerator.parcelableTypes) -> {
                containParcelable = true
                implementMethods.add(
                    GetterFunSpecBuilder(
                        componentType,
                        false,
                        null,
                        factory.generateFactoryCodeForParcelable()
                    )
                )
            }
            typeMirror.isSubType(absGenerator.parentListenerTypes) -> {
                containParentListener = true
                implementMethods.add(
                    GetterFunSpecBuilder(
                        componentType,
                        false,
                        null,
                        factory.generateFactoryCodeForParentListener()
                    )
                )
            }
            typeMirror.isSubType(absGenerator.viewBindingTypes) -> {
                containViewBindingProvider = true
                implementMethods.add(
                    GetterFunSpecBuilder(
                        componentType,
                        false,
                        null,
                        factory.generateFactoryCodeForViewBinding(componentType)
                    )
                )
            }
            else -> absGenerator.generateOtherDependentFunction(param).also(classSpec::addFunction)
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
}