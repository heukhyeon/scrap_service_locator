package kr.heukhyeon.service_locator

import kr.heukhyeon.service_locator.provider.IProvider
import java.util.*
import kotlin.reflect.KClass

interface IComponentModule {

    val scopeMap: HashMap<ComponentOwner, Scope>

    data class Scope(val instanceEntry: HashMap<Key, Any>)

    data class Key(val bindClazz: KClass<out Any>, val qualifier: String = "")

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> getCachedInstance(owner: ComponentOwner, key: Key): T? {
        if (scopeMap.containsKey(owner).not()) return null

        return scopeMap[owner]!!.instanceEntry[key] as? T
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any, R : T> cachingAndReturn(owner: ComponentOwner, key: Key, instance: T): T {
        val scope = scopeMap[owner] ?: Scope(hashMapOf()).also {
            scopeMap[owner] = it
        }
        scope.instanceEntry[key] = instance
        return instance
    }

    companion object {
        val SINGLETON_OWNER = object : ComponentOwner {
            override val providerBuffer: LinkedList<IProvider<*>> = LinkedList()
        }
    }
}