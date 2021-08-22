package kr.heukhyeon.service_locator.provider

import kr.heukhyeon.service_locator.ComponentOwner
import kr.heukhyeon.service_locator.RootInjector
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * [kr.heukhyeon.service_locator.ComponentOwner.providerInit] 가 호출되었을때 컴포넌트 주입을 실행하는 클래스.
 * 일반적으로 사용을 권장하지만, 특정 케이스에 따라 [FactoryProvider] 나 [ReactingProvider] 를 사용해야할수도 잇다.
 *
 * [kr.heukhyeon.service_locator.ComponentOwner.providerInit] 가 호출되기전에 객체 요청이 발생하는경우 [IllegalStateException] 가 발생한다.
 */
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