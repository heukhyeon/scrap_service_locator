package kr.heukhyeon.annotation_processor

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeVariableName
import kr.heukhyeon.service_locator.ComponentOwner
import javax.annotation.processing.ProcessingEnvironment

class AbstractCodeGenerator(environment: ProcessingEnvironment) : CodeCreateHelper(environment) {

    val parcelableTypes = getType("android.os.Parcelable")
    val viewBindingTypes = getType("androidx.viewbinding.ViewBinding")
    val fragmentParentListenerTypes = getType("kr.heukhyeon.service_locator.fragment.FragmentParentListener")
    val viewHolderParentListenerTypes = getType("kr.heukhyeon.service_locator.recyclerview.ViewHolderParentListener")

    private val bindingProviderType =
        getType("kr.heukhyeon.service_locator.viewbinding.ViewBindingProvider")

    fun generateAbstractGetterParcelableFunction(): FunSpec {
        val bounds = parcelableTypes!!.toClassName()
        return FunSpec.builder("getParcelable")
            .addModifiers(KModifier.SUSPEND)
            .addModifiers(KModifier.ABSTRACT)
            .addTypeVariable(TypeVariableName.Companion.invoke("T", bounds))
            .addParameter("owner", ComponentOwner::class)
            .returns(TypeVariableName.invoke("T"))
            .build()
    }

    fun generateAbstractGetterParentListenerFunction(): FunSpec {
        val bounds = fragmentParentListenerTypes!!.toClassName()
        return FunSpec.builder("getFragmentParentListener")
            .addModifiers(KModifier.SUSPEND)
            .addModifiers(KModifier.ABSTRACT)
            .addTypeVariable(TypeVariableName.Companion.invoke("T", bounds))
            .addParameter("owner", ComponentOwner::class)
            .returns(TypeVariableName.invoke("T"))
            .build()
    }

    fun generateAbstractGetterViewHolderParentListenerFunction(): FunSpec {
        val bounds = viewHolderParentListenerTypes!!.toClassName()
        return FunSpec.builder("getViewHolderParentListener")
            .addModifiers(KModifier.SUSPEND)
            .addModifiers(KModifier.ABSTRACT)
            .addTypeVariable(TypeVariableName.Companion.invoke("T", bounds))
            .addParameter("owner", ComponentOwner::class)
            .returns(TypeVariableName.invoke("T"))
            .build()
    }

    fun generateAbstractGetterViewBindingProviderFunction(): FunSpec {
        return FunSpec.builder("getViewBindingProvider")
            .addModifiers(KModifier.SUSPEND)
            .addModifiers(KModifier.ABSTRACT)
            .addParameter("owner", ComponentOwner::class)
            .returns(bindingProviderType!!.toClassName())
            .build()
    }

    fun generateOtherDependentFunction(param: GetterFunSpecBuilder.Parameter): FunSpec {
        return FunSpec.builder(
            createGetterFunctionName(
                param.typeName,
                param.qualifier?.annotationType
            )
        )
            .addModifiers(KModifier.SUSPEND)
            .addParameter("owner", ComponentOwner::class)
            .returns(param.typeName)
            .addModifiers(KModifier.ABSTRACT)
            .build()
    }
}