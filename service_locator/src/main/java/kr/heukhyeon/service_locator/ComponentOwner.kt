package kr.heukhyeon.service_locator

import kotlinx.coroutines.runBlocking
import kr.heukhyeon.service_locator.provider.FactoryProvider
import kr.heukhyeon.service_locator.provider.IProvider
import kr.heukhyeon.service_locator.provider.Provider
import kr.heukhyeon.service_locator.provider.ReactingProvider
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

    /**
     * [providerInit] 때 컴포넌트를 주입받아 사용할수있는 [IProvider] 를 반환한다.
     */
    fun <T : Any> inject(clazz: KClass<T>): IProvider<T> {
        val instance = Provider(clazz)
        providerBuffer.add(instance)
        return instance
    }

    /**
     * 필요할때마다 [FactoryProvider.Factory.create] 를 호출해 서로 다른 컴포넌트를 사용할수있는 [IProvider] 를 반환한다.
     */
    fun <T : Any> factory(clazz: KClass<T>): FactoryProvider<T> {
        val instance = FactoryProvider(clazz)
        providerBuffer.add(instance)
        return instance
    }

    /**
     * 되도록이면 [providerInit] 때 컴포넌트를 주입받되, 불가피할경우 객체 접근 요청이 이루어졌을시에도
     * 컴포넌트를 생성할수잇는 [IProvider] 를 반환한다.
     *
     * 이 함수의 주의 사항은 [kr.heukhyeon.service_locator.provider.ReactingProvider] 을 참고할것.
     */
    fun <T : Any> injectReacting(clazz: KClass<T>): IProvider<T> {
        val instance = ReactingProvider(clazz)
        providerBuffer.add(instance)
        return instance
    }

}