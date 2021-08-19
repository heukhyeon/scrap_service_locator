package kr.heukhyeon.service_locator

import kotlinx.coroutines.runBlocking
import kr.heukhyeon.service_locator.provider.FactoryProvider
import kr.heukhyeon.service_locator.provider.IProvider
import kr.heukhyeon.service_locator.provider.Provider
import java.util.*
import kotlin.reflect.KClass

/**
 * The interface of the smallest unit
 * that the class owning the component must implement, such as Activity and Fragment.
 *
 * This interface doesn't do anything other than dependency injection,
 * Asynchronously performing the functions of this interface is handled by [Initializer], which is a sub-interface.
 */
interface ComponentOwner {

    val providerBuffer: LinkedList<IProvider<*>>


    suspend fun providerInit() {
        providerBuffer.forEach {
            it.inject(this)
        }
    }

    fun dispose() {
        providerBuffer.forEach {
            it.finalize()
        }
        RootInjector.onDisposeComponentOwner(this)
    }

    fun <T : Any> inject(clazz: KClass<T>, injectImmediate: Boolean = false): IProvider<T> {
        val instance = Provider(clazz)
        providerBuffer.add(instance)
        if (injectImmediate) runBlocking { instance.inject(this@ComponentOwner) }
        return instance
    }

    fun <T : Any> factory(clazz: KClass<T>): FactoryProvider<T> {
        val instance = FactoryProvider(clazz)
        providerBuffer.add(instance)
        return instance
    }
}