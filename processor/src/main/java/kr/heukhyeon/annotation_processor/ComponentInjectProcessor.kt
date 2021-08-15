package kr.heukhyeon.annotation_processor

import com.squareup.kotlinpoet.*
import kr.heukhyeon.service_locator.Component
import kr.heukhyeon.service_locator.ComponentModule
import kr.heukhyeon.service_locator.EntryPoint
import kr.heukhyeon.service_locator.IComponentModule
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

class ComponentInjectProcessor : AbstractProcessor() {

    private lateinit var processingEnvironment: ProcessingEnvironment

    override fun init(p0: ProcessingEnvironment?) {
        super.init(p0)
        p0 ?: return
        processingEnvironment = p0
    }

    override fun process(p0: MutableSet<out TypeElement>?, p1: RoundEnvironment?): Boolean {
        val componentList = p1?.getElementsAnnotatedWith(Component::class.java)
        val entryPointList = p1?.getElementsAnnotatedWith(EntryPoint::class.java)
        if (componentList.isNullOrEmpty() && entryPointList.isNullOrEmpty()) return false

        val rootFile = processingEnvironment.options["kapt.kotlin.generated"]?.replace("kaptKotlin", "kapt")
            ?.let { File(it) }!!
        val moduleName = requireNotNull(processingEnvironment.options["moduleName"])

        val fileSpec = FileSpec.builder("io.anonymous.module", moduleName)
        val classSpec = TypeSpec.interfaceBuilder(moduleName)
            .addSuperinterface(IComponentModule::class)
            .addAnnotation(
                AnnotationSpec.builder(ComponentModule::class.java)
                    .build()
            )

        ModuleCreator(classSpec, processingEnvironment, p1).create()

        fileSpec
            .addType(classSpec.build())
            .build().writeTo(rootFile)
        return false
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Component::class.java.canonicalName, EntryPoint::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }
}