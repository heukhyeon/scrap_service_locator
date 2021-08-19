package kr.heukhyeon.service_locator

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Component(val scope : ComponentScope = ComponentScope.SHARED_IF_EQUAL_OWNER, val bind: KClass<*> = Any::class)
