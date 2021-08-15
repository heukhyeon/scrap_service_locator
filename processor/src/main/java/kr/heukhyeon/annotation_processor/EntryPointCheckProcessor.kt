package kr.heukhyeon.annotation_processor

import kr.heukhyeon.service_locator.ApplicationEntryPoint
import kr.heukhyeon.service_locator.ComponentModule
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

class EntryPointCheckProcessor : AbstractProcessor() {

    private lateinit var env: ProcessingEnvironment
    private lateinit var rootFile: File
    private var isCreatedRootInjector = false

    override fun init(p0: ProcessingEnvironment?) {
        super.init(p0)
        env = p0 ?: return
        rootFile = p0.options["kapt.kotlin.generated"]?.replace("kaptKotlin", "kapt")
            ?.let { File(it) }!!
    }

    override fun process(p0: MutableSet<out TypeElement>?, p1: RoundEnvironment?): Boolean {
//        p1 ?: return false
//        println("Types : $p0")
//        val appEntryPoint = p1.getElementsAnnotatedWith(ApplicationEntryPoint::class.java)
//        val componentModule = p1.getElementsAnnotatedWith(ComponentModule::class.java)
//
//        if (appEntryPoint.isEmpty() && isCreatedRootInjector.not()) return false
//        require(appEntryPoint.size == 1) {
//            "Size : ${appEntryPoint.size} / ${componentModule.size}"
//        }
//
//        if (componentModule.isEmpty() && isCreatedRootInjector) return false
//
//        isCreatedRootInjector = true
//
//        require(componentModule.isEmpty() || componentModule.size == 1)
//
//        println("Exists : ${componentModule.size}")
//
//        RootInjectCreator(componentModule.firstOrNull()).create(rootFile)

        return false
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            ApplicationEntryPoint::class.java.canonicalName,
            ComponentModule::class.java.canonicalName
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

}