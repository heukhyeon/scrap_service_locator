package kr.heukhyeon.service_locator.provider

import kr.heukhyeon.service_locator.ComponentOwner
import kotlin.reflect.KProperty

interface IProvider<T> {

    suspend fun inject(owner: ComponentOwner)

    fun finalize()

    operator fun getValue(thisRef: ComponentOwner, property: KProperty<*>): T

}