package kr.heukhyeon.service_locator.provider

import kotlinx.coroutines.runBlocking
import kr.heukhyeon.service_locator.ComponentOwner
import kr.heukhyeon.service_locator.RootInjector
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class FactoryProvider<T : Any>(private val clazz:KClass<T>) : IProvider<FactoryProvider.Factory<T>> {

    class Factory<T : Any>(private var owner: ComponentOwner?, private val clazz: KClass<T>) {

        fun create(): T {
            return runBlocking {
                RootInjector.get(requireNotNull(owner), clazz)
            }
        }

        fun finalize() {
            owner = null
        }
    }

    private var factory : Factory<T>? = null

    override suspend fun inject(owner: ComponentOwner) {
        factory = Factory(owner, clazz)
    }

    override fun finalize() {
        factory?.finalize()
        factory = null
    }

    override operator fun getValue(thisRef: ComponentOwner, property: KProperty<*>): Factory<T> {
        return requireNotNull(factory)
    }
}