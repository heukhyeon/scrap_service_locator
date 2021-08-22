package kr.heukhyeon.service_locator.provider

import kotlinx.coroutines.runBlocking
import kr.heukhyeon.service_locator.ComponentOwner
import kr.heukhyeon.service_locator.RootInjector
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Activity 나 Fragment 의 로딩용 화면등, 정말로 객체 접근 요청이 들어왔을때 바로 객체 생성이 필요한경우에 사용.
 * View 에 직접적으로 제어되는 컴포넌트 외에는 사용을 지양해야하며, View 라 해도 해당 뷰가 충분히 가볍지않다면 다른 가벼운 뷰를 이 클래스를 사용해 주입받고,
 * 정말 필요한 View 는 [Provider] 를 통해 주입받은뒤 UI가 유휴상태일때 바꿔치는 방법을 고려해야한다.
 */
class ReactingProvider<T : Any>(val clazz: KClass<T>) : IProvider<T> {

    private var mInstance: T? = null
    private var isInitialized = false

    val instance: T?
        get() = mInstance

    override suspend fun inject(owner: ComponentOwner) {
        if (isInitialized.not()) {
            mInstance = RootInjector.get(owner, clazz)
        }
    }

    override fun finalize() {
        mInstance = null
        isInitialized = false

    }

    override operator fun getValue(thisRef: ComponentOwner, property: KProperty<*>): T {
        if (isInitialized.not()) {
            isInitialized = true
            mInstance = runBlocking {
                RootInjector.get(thisRef, clazz)
            }
        }
        return requireNotNull(instance)
    }
}