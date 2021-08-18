package kr.heukhyeon.annotation_processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import java.util.*
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

interface ICodeCreateHelper {

    fun viewBindingToPackageAndLayoutId(name: ClassName): String {
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

    fun Element.toClassName(): ClassName {
        return ClassName.bestGuess(toString())
    }

    fun TypeMirror.toClassName(): ClassName {
        return ClassName.bestGuess(toString())
    }

    fun ClassName.realName(containDot: Boolean) =
        simpleNames.joinToString(if (containDot) "." else "")

    fun FunSpec.Builder.addCodeWithTab(
        tabCount: Int,
        format: String,
        vararg args: Any
    ): FunSpec.Builder {
        return addCode(CodeBlock.builder().addCodeWithTab(tabCount, format, *args).build())
    }

    fun CodeBlock.Builder.addCodeWithTab(
        tabCount: Int,
        format: String,
        vararg args: Any
    ): CodeBlock.Builder {
        return add("${(0 until tabCount).map { '\t' }.joinToString("")}$format", *args)
    }

    fun createGetterFunctionName(type: ClassName, qualifier: TypeMirror?): String {
        val postfix = if(qualifier == null) "" else "With${qualifier.toClassName().realName(false)}"
        return "get${type.realName(false)}$postfix"
    }
}