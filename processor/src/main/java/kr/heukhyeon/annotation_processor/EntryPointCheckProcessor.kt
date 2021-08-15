package kr.heukhyeon.annotation_processor

import kr.heukhyeon.service_locator.ApplicationEntryPoint
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

class EntryPointCheckProcessor : AbstractProcessor() {

    private lateinit var env: ProcessingEnvironment
    private lateinit var rootFile: File

    override fun init(p0: ProcessingEnvironment?) {
        super.init(p0)
        env = p0 ?: return
        rootFile = p0.options["kapt.kotlin.generated"]?.replace("kaptKotlin", "kapt")
            ?.let { File(it) }!!
    }

    override fun process(p0: MutableSet<out TypeElement>?, p1: RoundEnvironment?): Boolean {
        p1 ?: return false
        if (p0.isNullOrEmpty()) return false

        val appEntryPoint = p1.getElementsAnnotatedWith(ApplicationEntryPoint::class.java)

        if (appEntryPoint.isEmpty()) return false

        require(appEntryPoint.size == 1)

        RootInjectCreator().create(rootFile)

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