package kr.heukhyeon.annotation_processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import kr.heukhyeon.service_locator.Component
import kr.heukhyeon.service_locator.ComponentQualifier
import java.lang.NullPointerException
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

open class CodeCreateHelper(protected val runtimeEnv: ProcessingEnvironment) : ICodeCreateHelper {

    /**
     * Null 인경우 포함이 안된경우 (예 : 안드로이드 비의존적인 모듈에서 DataBinding 참조 등)임
     */
    protected fun getType(canonicalName:CharSequence) : TypeMirror? {
        return try {
            runtimeEnv.elementUtils.getTypeElement(canonicalName).asType()
        }
        catch (e: NullPointerException) {
            null
        }
    }

    protected fun TypeMirror.isSubType(superType:TypeMirror?): Boolean {
        if (superType == null) return false

        return runtimeEnv.typeUtils.isSubtype(this, superType)
    }

    private val componentAnnotation = runtimeEnv.elementUtils.getTypeElement(
        Component::class.java.canonicalName
    ).asType()

    /**
     * 현재 앨리먼트에 [ComponentQualifier] 가 붙은 어노테이션이 추가로 붙어있는지를 확인한다.
     */
    protected fun Element.getComponentQualifier(): AnnotationMirror? {
        return annotationMirrors
            .filter { runtimeEnv.typeUtils.isSameType(it.annotationType, componentAnnotation).not() }
            .firstOrNull { annotation ->
            annotation.annotationType.asElement().getAnnotation(ComponentQualifier::class.java) != null
        }
    }
}