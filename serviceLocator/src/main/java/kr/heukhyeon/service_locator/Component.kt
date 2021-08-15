package kr.heukhyeon.service_locator

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Component(val isSingleton: Boolean = false, val bind: KClass<*> = Any::class)
