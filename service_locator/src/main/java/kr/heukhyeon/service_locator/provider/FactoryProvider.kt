package kr.heukhyeon.service_locator.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kr.heukhyeon.service_locator.ComponentOwner
import kr.heukhyeon.service_locator.FakeComponentOwner
import kr.heukhyeon.service_locator.IComponentModule
import kr.heukhyeon.service_locator.RootInjector
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * RecyclerView.ViewHolder 등 어떤 특정 함수가 자주 불릴거라 예측되고, 함수 호출때마다 고유한 컴포넌트가 반환되어야 할때 사용한다.
 * [Provider], [ReactingProvider] 와 달리 파라미터로 넘어온 클래스를 직접 반환하지않고 해당 타입을 가진 [Factory] 를 반환하며,
 * 실제 객체 생성은 이 클래스를 통해 접근가능한 [Factory.create] 함수를 호출시킨다.
 *
 * [getValue] 가 호출될때마다 자체적으로 컴포넌트 생성을 해서 반환시킬수도 있지만, 이렇게 할경우 사용자 입장에서 해당 객체가
 * 접근할때마다 달라질수있음을 간과하기 쉬우므로 간접적인 방법을 사용한다.
 */
class FactoryProvider<T : Any>(private val clazz:KClass<T>) : IProvider<FactoryProvider.Factory<T>> {

    class Factory<T : Any>(private val owner: ComponentOwner, private val clazz: KClass<T>) {

        fun create(): T {
            return runBlocking(Dispatchers.Default) {
                val owner = FakeComponentOwner(realComponentOwner = owner)
                val instance = RootInjector.get(owner, clazz)
                owner.dispose()
                instance
            }
        }
    }

    private var factory : Factory<T>? = null

    override suspend fun inject(owner: ComponentOwner) {
        factory = Factory(owner, clazz)
    }

    override fun finalize() {
        factory = null
    }

    override operator fun getValue(thisRef: ComponentOwner, property: KProperty<*>): Factory<T> {
        return requireNotNull(factory)
    }
}