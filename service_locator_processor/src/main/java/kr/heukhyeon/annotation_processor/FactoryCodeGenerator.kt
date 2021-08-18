package kr.heukhyeon.annotation_processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement

class FactoryCodeGenerator(environment: ProcessingEnvironment) : CodeCreateHelper(environment) {

    fun generateFactoryCodeForNormal(element: Element,
                                     notImplementedMethods: MutableList<GetterFunSpecBuilder.Parameter>): CodeBlock {
        val constructor =
            element.enclosedElements.first { it.kind == ElementKind.CONSTRUCTOR } as ExecutableElement

        val params = constructor.parameters.map {
            GetterFunSpecBuilder.Parameter(it, it.getComponentQualifier())
        }

        notImplementedMethods.addAll(params)

        return CodeBlock.builder()
            .add("{\n")
            .addCodeWithTab(3, "%T(\n", element.asType())
            .also { block ->
                params.forEach { param ->
                    val methodName = createGetterFunctionName(param.typeName, param.qualifier?.annotationType)
                    block.addCodeWithTab(4, "${param.name} = $methodName(owner),\n")
                }
            }
            .addCodeWithTab(3, ")\n")
            .addCodeWithTab(2, "}\n")
            .build()
    }

    /**
     * ViewBinding 을 상속하는 타입에 대한 객체 생성 함수를 정의한다.
     */
    fun generateFactoryCodeForViewBinding(targetType: ClassName): CodeBlock {
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
    fun generateFactoryCodeForParcelable(): CodeBlock {
        return CodeBlock.builder()
            .add("{\n")
            .addCodeWithTab(3, "getParcelable(owner)\n")
            .addCodeWithTab(2, "}\n")
            .build()
    }

    /**
     * FragmentParentListener 를 상속하는 타입에 대한 객체 생성 함수를 정의한다.
     */
    fun generateFactoryCodeForParentListener(): CodeBlock {
        return CodeBlock.builder()
            .add("{\n")
            .addCodeWithTab(3, "getFragmentParentListener(owner)\n")
            .addCodeWithTab(2, "}\n")
            .build()
    }
}