package kr.heukhyeon.service_locator.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kr.heukhyeon.service_locator.ComponentOwner
import kr.heukhyeon.service_locator.IComponentModule
import kr.heukhyeon.service_locator.RootInjector
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class FactoryProvider<T : Any>(private val clazz:KClass<T>) : IProvider<FactoryProvider.Factory<T>> {

    class Factory<T : Any>(private val clazz: KClass<T>) {

        fun create(): T {
            return runBlocking(Dispatchers.Default) {
                val owner = object : ComponentOwner {
                    override val providerBuffer: LinkedList<IProvider<*>>
                        get() = LinkedList()
                }
                val instance = RootInjector.get(IComponentModule.NOT_CACHED_OWNER, clazz)
                owner.dispose()
                instance
            }
        }
    }

    private var factory : Factory<T>? = null

    override suspend fun inject(owner: ComponentOwner) {
        factory = Factory(clazz)
    }

    override fun finalize() {
        factory = null
    }

    override operator fun getValue(thisRef: ComponentOwner, property: KProperty<*>): Factory<T> {
        return requireNotNull(factory)
    }
}