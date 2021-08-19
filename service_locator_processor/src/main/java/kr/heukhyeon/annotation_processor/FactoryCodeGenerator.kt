package kr.heukhyeon.annotation_processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import kr.heukhyeon.service_locator.provider.FactoryProvider
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
            .addCodeWithTab(3, "%T(\n", element.asType())
            .also { block ->
                params.forEach { param ->
                    val methodName = createGetterFunctionName(param.typeName, param.qualifier?.annotationType)
                    /**
                     * FactoryProvider.Factory 는 너무 광범위하게 구현되버릴수있어 예외적으로 inline 시킨다.
                     */
                    if (param.typeName is ParameterizedTypeName && param.typeName.rawType.canonicalName == FactoryProvider.Factory::class.java.canonicalName) {
                        notImplementedMethods.remove(param)
                        block.addCodeWithTab(4, "${param.name} = getFactory(%T::class),\n", param.typeName.typeArguments.first())
                    }
                    else {
                        block.addCodeWithTab(4, "${param.name} = $methodName(owner),\n")
                    }
                }
            }
            .addCodeWithTab(3, ")\n")
            .build()
    }

    /**
     * ViewBinding 을 상속하는 타입에 대한 객체 생성 함수를 정의한다.
     */
    fun generateFactoryCodeForViewBinding(targetType: TypeName): CodeBlock {
        return CodeBlock.builder()
            .addCodeWithTab(3, "getViewBindingProvider(owner).create(\n")
            .addCodeWithTab(4, "layoutId = ${viewBindingToPackageAndLayoutId(targetType)},\n")
            .addCodeWithTab(4, "factory = ${targetType.realName(false)}::bind\n")
            .addCodeWithTab(3, ")\n")
            .build()
    }

    /**
     * Parcelable 을 상속하는 타입에 대한 객체 생성 함수를 정의한다.
     */
    fun generateFactoryCodeForParcelable(): CodeBlock {
        return CodeBlock.builder()
            .addCodeWithTab(3, "getParcelable(owner)\n")
            .build()
    }

    /**
     * FragmentParentListener 를 상속하는 타입에 대한 객체 생성 함수를 정의한다.
     */
    fun generateFactoryCodeForParentListener(): CodeBlock {
        return CodeBlock.builder()
            .addCodeWithTab(3, "getFragmentParentListener(owner)\n")
            .build()
    }
}