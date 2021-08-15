package kr.heukhyeon.annotation_processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kr.heukhyeon.service_locator.ComponentOwner
import kr.heukhyeon.service_locator.IComponentModule
import kr.heukhyeon.service_locator.RootInjector
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.FilterBuilder
import java.io.File
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class RootInjectCreator {

    private val classSpec = TypeSpec.classBuilder("RootInjectorImpl")
        .addSuperinterface(RootInjector::class)

    private val modules = createReflections().getSubTypesOf(IComponentModule::class.java)
    private val rootInterfaceImplementMethods = IComponentModule::class.java.declaredMethods.map {
        it.name
    }

    // "A::class -> getA()" String List
    private val conditionStatements = ArrayList<String>()

    fun create(rootFile: File) {
        /**
         * 모듈 인터페이스 부여
         * ex : class RootInjector : ModuleA, ModuleB, ModuleC ....
         */
        modules.forEach { moduleClazz ->
            classSpec.addSuperinterface(moduleClazz)
        }

        generateConstructor()

        generateSingletonMap()

        /**
         * 모듈별 리턴함수 구성
         */
        modules.forEach {
            generateGetterFunction(it)
        }

        generateGetContextFunction()

        generateGetInstanceOverride()

        generateGetCoroutineScopeFunction()

        FileSpec.builder("kr.heukhyeon.service_locator", "RootInjectorImpl")
            .addType(classSpec.build())
            .build()
            .writeTo(rootFile)
    }

    private fun createReflections(): Reflections {
        return Reflections(
            SubTypesScanner(),
            FilterBuilder().includePackage("io.anonymous.module")
        )
    }

    /**
     * ApplicationContext를 받는 생성자를 정의한다.
     */
    private fun generateConstructor() {
        val type = ClassName.bestGuess("android.content.Context")
        FunSpec.constructorBuilder()
            .addParameter(
                "applicationContext",
                type,
                KModifier.PRIVATE
            )
            .build()
            .also(classSpec::primaryConstructor)

        PropertySpec.builder("applicationContext", type, KModifier.PRIVATE)
            .initializer("applicationContext")
            .build()
            .also(classSpec::addProperty)
    }

    /**
     * 싱글턴 객체를 캐싱하는 맵 필드를 만든다.
     */
    private fun generateSingletonMap() {
        PropertySpec.builder(
            "scopeMap",
            HashMap::class.parameterizedBy(
                    ComponentOwner::class,
                    IComponentModule.Scope::class
                )
        )
            .addModifiers(KModifier.OVERRIDE)
            .initializer("HashMap()")
            .build()
            .also(classSpec::addProperty)
    }

    private fun generateGetterFunction(module: Class<out IComponentModule>) {
        getDefaultImplementedFunctions(module).forEach { method ->
            val name = method.name

            FunSpec.builder(method.name)
                .addModifiers(KModifier.OVERRIDE)
                .addModifiers(KModifier.SUSPEND)
                .also { spec ->
                    method.typeParameters.forEach {
                        spec.addTypeVariable(it.asTypeVariableName())
                    }
                }
                .addParameter("owner", ComponentOwner::class)
                .returns(method.returnType.asTypeName())
                .addStatement("return super<${module.simpleName}>.$name(owner)")
                .build()
                .also(classSpec::addFunction)

            if (method.typeParameters.isEmpty()){
                conditionStatements.add("${method.returnType}::class -> ${method.name}(owner)")
            }
        }

    }

    private fun generateGetContextFunction() {
        FunSpec.builder("getContext")
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(KModifier.SUSPEND)
            .addParameter("owner", ComponentOwner::class)
            .addKdoc("It is Application Context\n")
            .returns(ClassName.bestGuess("android.content.Context"))
            .addStatement("return applicationContext")
            .build()
            .also(classSpec::addFunction)
    }

    private fun generateGetCoroutineScopeFunction() {
        FunSpec.builder("getCoroutineScope")
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(KModifier.SUSPEND)
            .addParameter("owner", ComponentOwner::class)
            .returns(ClassName.bestGuess("kotlinx.coroutines.CoroutineScope"))
            .addStatement("return owner.getCoroutineScope()")
            .build()
            .also(classSpec::addFunction)
    }

    /**
     * [RootInjector.getInstance] 에 대한 오버라이드 함수
     */
    private fun generateGetInstanceOverride() {
        FunSpec.builder("getInstance")
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(KModifier.SUSPEND)
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S, %S", "IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
                    .build()
            )
            .addParameter("owner", ComponentOwner::class)
            .addParameter(
                ParameterSpec.builder(
                    "clazz", KClass::class.asTypeName().parameterizedBy(
                        TypeVariableName.invoke("T")
                    )
                ).build()
            )
            .addTypeVariable(TypeVariableName.invoke("T", Any::class))
            .beginControlFlow("val instance = when(clazz)")
            .also { spec ->
                conditionStatements.forEach {
                    spec.addStatement(it)
                }
                spec.addStatement("else -> throw IllegalStateException(\"Not Implemented : \${clazz.qualifiedName} \")")
            }
            .endControlFlow()
            .addStatement("return instance as T")
            .returns(TypeVariableName.invoke("T", Any::class.java))
            .build()
            .also(classSpec::addFunction)
    }

    /**
     * kotlin 1.5 기준으로, 기존 Java Interface 의 함수에 대해 기본 구현이 되었는지 여부를 표시하는 [java.lang.reflect.Method.isDefault] 는
     * 언제나 false 다.
     *
     * 이는 kotlin이 인터페이스에 대해 java 코드로 변환시 java 1.8의 기본 구현 기능 대신
     * DefaultImpl 이라는 정적 클래스를 사용해 기본 구현을 처리하기때문인데,
     *
     * 이때문에 kotlin interface 의 기본 구현 함수를 추출하는건 별도의 과정을 거쳐야한다.
     */
    private fun getDefaultImplementedFunctions(module: Class<out IComponentModule>): List<KCallable<*>> {

        println("Module : ${module.canonicalName}")
        /**
         * 디컴파일시 확인되는 inner static class 의 이름을 추가로 입력해 클래스 객체를 뽑아온다.
         */
        val defaultImplClazz = Class.forName(module.canonicalName + "\$DefaultImpls")

        /**
         * 디컴파일된 DefaultImpl에 "정의된 함수" == 원래 인터페이스에서 "기본 구현된 함수"
         */
        val defaultMethods = defaultImplClazz.declaredMethods.map { it.name }

        /**
         * DeclaredMethod가 아닌 일반 Methods 를 사용하면 상위 인터페이스에 있는 기본구현 함수까지 모두 딸려온다.
         */
        return module.kotlin.members.filter {
            println("in ${module.canonicalName} : ${it.name}")
            it.isOpen && defaultMethods.contains(it.name) && rootInterfaceImplementMethods.contains(it.name).not()
        }
    }
}