package kr.heukhyeon.service_locator.viewbinding

import androidx.viewbinding.ViewBinding
import kr.heukhyeon.service_locator.initializer.AndroidInitializer
import kr.heukhyeon.service_locator.provider.Provider
import java.lang.IllegalStateException

/**
 * 현재 객체에 주입된 ViewBinding 목록을 가져온다.
 * [AndroidInitializer.onInitialize] 호출전에 호출할경우 [IllegalStateException] 를 발생시킨다.
 */
fun AndroidInitializer.getViewBindings() : List<ViewBinding> {
    if (isInitialized.not()) throw IllegalStateException("You cannot call getViewBindings without object initialization complete.")

    return providerBuffer.mapNotNull {
        if (it is Provider && it.clazz.java.interfaces.contains(ViewBinding::class.java)) {
            it.instance as ViewBinding
        }
        else null
    }
}