package kr.heukhyeon.service_locator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kr.heukhyeon.service_locator.provider.Provider
import java.util.*
import kotlin.collections.HashMap
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass

interface IComponentModule {

    val scopeMap : HashMap<ComponentOwner, Scope>

    data class Scope(val instanceEntry : HashMap<KClass<out Any>, Any>)

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any, R : T> cachingAndReturn(owner: ComponentOwner, keyClazz: KClass<T>, factory: suspend () -> R): T {
        val scope = scopeMap[owner] ?: Scope(hashMapOf()).also {
            scopeMap[owner] = it
        }
        if (scope.instanceEntry.containsKey(keyClazz)) {
            return scope.instanceEntry[keyClazz] as T
        }
        return factory().also {
            scope.instanceEntry[keyClazz] = it
        }
    }

    companion object {
        val SINGLETON_OWNER = object : ComponentOwner {
            override val providerBuffer: LinkedList<Provider<*>> = LinkedList()

            override fun getCoroutineScope(): CoroutineScope {
                return CoroutineScope(EmptyCoroutineContext)
            }
        }
    }
}