package kr.heukhyeon.annotation_processor

import com.squareup.kotlinpoet.*
import kr.heukhyeon.service_locator.Component
import kr.heukhyeon.service_locator.ComponentOwner
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror


/**
 * 객체 주입 요청이 들어왔을때 해당 객체를 반환하는 함수를 추가한다.
 * @params
 * [returnType] : 반환되는 타입
 * [factory] : 해당 객체의 생성방법, 일반적으로 [generateFactoryCodeForNormal] 다.
 * [isSingleton] : [Component.isSingleton] 을 통해 획득한, 해당 객체가 싱글턴 객체로 존재해야하는지 여부
 */
class GetterFunSpecBuilder(
    private val returnType: ClassName,
    private val isSingleton: Boolean,
    private val qualifier: AnnotationMirror?,
    private val factory: CodeBlock
) : ICodeCreateHelper {

    private var isAdded = false

    class Parameter(
        val name: String,
        val type: TypeMirror,
        val qualifier: AnnotationMirror?
    ) {
        constructor(element: VariableElement, qualifier: AnnotationMirror?) : this(
            element.simpleName.toString(),
            element.asType(),
            qualifier
        )

        val typeName = ClassName.bestGuess(type.toString())
    }

    fun build(classSpec: TypeSpec.Builder) {
        if (isAdded) return

        val owner = if (isSingleton) "IComponentModule.SINGLETON_OWNER" else "owner"
        FunSpec.builder(createGetterFunctionName(returnType, qualifier?.annotationType))
            .addModifiers(KModifier.SUSPEND)
            .addParameter("owner", ComponentOwner::class)
            .addCode("return cachingAndReturn(\n")
            .addCodeWithTab(2, "owner = ${owner},\n")
            .addCodeWithTab(2, "keyClazz = %T::class,\n", returnType)
            .addCodeWithTab(2, "factory = ")
            .addCode(factory)
            .addCodeWithTab(1, ")")
            .returns(returnType)
            .build()
            .also(classSpec::addFunction)

        isAdded = true
    }
}