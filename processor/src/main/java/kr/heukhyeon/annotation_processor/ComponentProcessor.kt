package kr.heukhyeon.annotation_processor

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import kr.heukhyeon.service_locator.ApplicationEntryPoint
import kr.heukhyeon.service_locator.Component
import kr.heukhyeon.service_locator.EntryPoint
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

class ComponentProcessor : AbstractProcessor() {

    private lateinit var env: ProcessingEnvironment
    private lateinit var rootFile: File
    private var moduleName = ""
    private var generatedTypes = emptyList<TypeName>()

    override fun init(p0: ProcessingEnvironment?) {
        super.init(p0)
        env = p0 ?: return
        rootFile = p0.options["kapt.kotlin.generated"]?.replace("kaptKotlin", "kapt")
            ?.let { File(it) }!!
        moduleName = p0.options["moduleName"] ?: ""
    }

    override fun process(p0: MutableSet<out TypeElement>?, p1: RoundEnvironment?): Boolean {
        p1 ?: return false
        p0 ?: return false

        val appEntryPoint = p1.getElementsAnnotatedWith(ApplicationEntryPoint::class.java)
        val componentList = p1.getElementsAnnotatedWith(Component::class.java)
        val entryPointList = p1.getElementsAnnotatedWith(EntryPoint::class.java)

        if (componentList?.isNotEmpty() == true || entryPointList?.isNotEmpty() == true) {
            val moduleCreator = ModuleCreator(moduleName, env, p1)
            FileSpec.builder("io.anonymous.module", moduleName)
                .addType(moduleCreator.create())
                .build()
                .writeTo(rootFile)
            generatedTypes = moduleCreator.generatedTypes
        }


        if (appEntryPoint.isNotEmpty()) {
            require(appEntryPoint.size == 1)
            RootInjectCreator("io.anonymous.module.$moduleName", generatedTypes).create(rootFile)
        }
        return false
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            ApplicationEntryPoint::class.java.canonicalName
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

}