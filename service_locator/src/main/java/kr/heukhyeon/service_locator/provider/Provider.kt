package kr.heukhyeon.service_locator.provider

import kr.heukhyeon.service_locator.ComponentOwner
import kr.heukhyeon.service_locator.RootInjector
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class Provider<T : Any>(val clazz: KClass<T>) : IProvider<T> {

    private var mInstance: T? = null

    val instance: T?
        get() = mInstance

    override suspend fun inject(owner: ComponentOwner) {
        mInstance = RootInjector.get(owner, clazz)
    }

    override fun finalize() {
        mInstance = null
    }

    override operator fun getValue(thisRef: ComponentOwner, property: KProperty<*>): T {
        return requireNotNull(instance)
    }
}