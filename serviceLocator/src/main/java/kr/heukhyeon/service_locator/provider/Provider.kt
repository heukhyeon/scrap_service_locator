package kr.heukhyeon.service_locator.provider

import kr.heukhyeon.service_locator.ComponentOwner
import kr.heukhyeon.service_locator.RootInjector
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class Provider<T : Any>(val clazz: KClass<T>) {

    private var mInstance: T? = null

    val instance: T?
        get() = mInstance

    internal suspend fun inject(owner: ComponentOwner) {
        mInstance = RootInjector.get(owner, clazz)
    }

    fun finalize() {
        mInstance = null
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return requireNotNull(instance)
    }
}