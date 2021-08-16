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

        /**
         * 현재 어노테이션 프로세서가 실행중인 모듈에서 [kr.heukhyeon.service_locator.IComponentModule] 을 구현하는 모듈을 만들어야하는경우
         */
        if (componentList?.isNotEmpty() == true || entryPointList?.isNotEmpty() == true) {
            val moduleCreator = ModuleCreator(moduleName, env, p1)
            FileSpec.builder("io.anonymous.module", moduleName)
                .addType(moduleCreator.create())
                .build()
                .writeTo(rootFile)
            generatedTypes = moduleCreator.generatedTypes
        }

        /**
         * 현재 어노테이션 프로세서가 실행중인 모듈이 어플리케이션 모듈 ( = [ApplicationEntryPoint] 가 포함된 경우) 인 경우,
         * 실제 종속성 주입 처리를 실행하는 클래스를 작성한다.
         */
        if (appEntryPoint.isNotEmpty()) {
            require(appEntryPoint.size == 1)
            val applicationModuleName = if (generatedTypes.isNotEmpty()) "io.anonymous.module.$moduleName" else null
            RootInjectCreator(applicationModuleName, generatedTypes).create(rootFile)
        }
        return false
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            Component::class.java.canonicalName,
            EntryPoint::class.java.canonicalName,
            ApplicationEntryPoint::class.java.canonicalName
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

}