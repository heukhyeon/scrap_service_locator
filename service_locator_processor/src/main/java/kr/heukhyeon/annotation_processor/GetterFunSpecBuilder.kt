package kr.heukhyeon.annotation_processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kr.heukhyeon.service_locator.Component
import kr.heukhyeon.service_locator.ComponentOwner
import kr.heukhyeon.service_locator.ComponentScope
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror


/**
 * 객체 주입 요청이 들어왔을때 해당 객체를 반환하는 함수를 추가한다.
 * @params
 * [returnType] : 반환되는 타입
 * [factory] : 해당 객체의 생성방법, 일반적으로 [generateFactoryCodeForNormal] 다.
 * [scope] : 해당 객체의 중복 요청시에 대한 스코프 정책
 */
class GetterFunSpecBuilder(
    private val returnType: TypeName,
    private val scope: ComponentScope,
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

        val typeName = type.toString().let {
            if (it.contains("<").not()) return@let ClassName.bestGuess(it)

            val origin = it.substring(0, it.indexOf("<"))
            val param = it.substring(it.indexOf("<") + 1, it.lastIndexOf(">"))

            ClassName.bestGuess(origin).parameterizedBy(ClassName.bestGuess(param))
        }
    }

    fun build(classSpec: TypeSpec.Builder) {
        if (isAdded) return

        val owner = when (scope) {
            ComponentScope.SHARED_IF_EQUAL_OWNER -> "owner"
            ComponentScope.IS_SINGLETON -> "IComponentModule.SINGLETON_OWNER"
            ComponentScope.NOT_CACHED -> "IComponentModule.NOT_CACHED_OWNER"
        }
        FunSpec.builder(createGetterFunctionName(returnType, qualifier?.annotationType))
            .addModifiers(KModifier.SUSPEND)
            .addParameter("owner", ComponentOwner::class)
            .addStatement("val realOwner = $owner")
            .addStatement("val key = IComponentModule.Key(%T::class, %S)", returnType, qualifier?.toString() ?: "")
            .addCode("return getCachedInstance(realOwner, key) ?: ")
            .addCode("cachingAndReturn(realOwner, key,\n")
            .addCode(factory)
            .addCode(")")
            .returns(returnType)
            .build()
            .also(classSpec::addFunction)

        isAdded = true
    }
}