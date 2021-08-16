package kr.heukhyeon.annotation_processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import java.lang.NullPointerException
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

open class ClassCreator(protected val runtimeEnv: ProcessingEnvironment) {

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

    protected fun viewBindingToPackageAndLayoutId(name: ClassName): String {
        val regex = "([a-z])([A-Z]+)"
        val replacement = "$1_$2"
        val layoutId = name.simpleName
            .dropLast(7) // "Binding" 을 제거한다.
            .replace(
                Regex(regex),
                replacement
            )
            .lowercase(Locale.US)

        // packageName 에서 ".databinding" 을 제거한다.
        val packageName = name.packageName.dropLast(12)
        return "$packageName.R.layout.$layoutId"
    }

    protected fun TypeMirror.isSubType(superType:TypeMirror?): Boolean {
        if (superType == null) return false

        return runtimeEnv.typeUtils.isSubtype(this, superType)
    }



    protected fun Element.toClassName(): ClassName {
        return ClassName.bestGuess(toString())
    }

    protected fun TypeMirror.toClassName(): ClassName {
        return ClassName.bestGuess(toString())
    }

    protected fun ClassName.realName(containDot: Boolean) =
        simpleNames.joinToString(if (containDot) "." else "")

    protected fun FunSpec.Builder.addCodeWithTab(
        tabCount: Int,
        format: String,
        vararg args: Any
    ): FunSpec.Builder {
        return addCode(CodeBlock.builder().addCodeWithTab(tabCount, format, *args).build())
    }

    protected fun CodeBlock.Builder.addCodeWithTab(
        tabCount: Int,
        format: String,
        vararg args: Any
    ): CodeBlock.Builder {
        return add("${(0 until tabCount).map { '\t' }.joinToString("")}$format", *args)
    }
}