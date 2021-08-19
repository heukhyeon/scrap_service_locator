package kr.heukhyeon.service_locator

import kr.heukhyeon.service_locator.provider.IProvider
import java.util.*

/**
 * [kr.heukhyeon.service_locator.provider.FactoryProvider.Factory] 가 객체를 생성하는동안 추가적으로 생성되는 컴포넌트들이
 * 동일한 객체 참조를 가지게 하기위한 임시 Owner, 의존성 주입이 완료되고나면 dispose 된다.
 * [realComponentOwner] : [kr.heukhyeon.service_locator.provider.FactoryProvider.Factory] 를 생성한 Owner.
 * 이게 필요한 이유는, owner를 직접 캐스팅해 쓰는경우 (ViewHolderParentListener 등) 가 있기 때문이다.
 */
class FakeComponentOwner internal constructor(val realComponentOwner: ComponentOwner) : ComponentOwner {

    init {
        require(realComponentOwner !is FakeComponentOwner)
    }

    override val providerBuffer: LinkedList<IProvider<*>>
        get() = LinkedList()
}