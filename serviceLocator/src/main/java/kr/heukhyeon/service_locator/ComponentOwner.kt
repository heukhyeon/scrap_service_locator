package kr.heukhyeon.service_locator

import kr.heukhyeon.service_locator.provider.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.reflect.KClass


interface ComponentOwner {

    val providerBuffer: LinkedList<Provider<*>>

    fun getCoroutineScope() : CoroutineScope

    suspend fun providerInit() {
        providerBuffer.forEach {
            it.inject(this)
        }
    }

    fun dispose() {
        getCoroutineScope().coroutineContext.cancelChildren()
        providerBuffer.forEach {
            it.finalize()
        }
        RootInjector.onDisposeComponentOwner(this)
    }

    fun <T : Any> inject(clazz: KClass<T>, injectImmediate:Boolean = false): Provider<T> {
        val instance = Provider(clazz)
        providerBuffer.add(instance)
            if (injectImmediate) runBlocking { instance.inject(this@ComponentOwner) }
            return instance

    }
}