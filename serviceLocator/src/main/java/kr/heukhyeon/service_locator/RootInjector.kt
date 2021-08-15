package kr.heukhyeon.service_locator

import kotlin.reflect.KClass

interface RootInjector : IComponentModule {

    suspend fun <T : Any> getInstance(owner: ComponentOwner, clazz: KClass<T>): T

    fun onDisposeComponentOwner(owner: ComponentOwner) {
        scopeMap[owner]?.instanceEntry?.clear()
        scopeMap.remove(owner)
    }

    companion object {
        private var instance: RootInjector? = null

        internal suspend fun <T : Any> get(owner: ComponentOwner, clazz: KClass<T>): T {
            return requireNotNull(instance) {
                "RootInjector is Not Initialized. Make sure you call RootInjector.initialize(this) from your Application class."
            }.getInstance(owner, clazz)
        }

        internal fun onDisposeComponentOwner(owner: ComponentOwner) {
            instance?.onDisposeComponentOwner(owner)
        }

        fun initialize(context: Any) {
            val impl = try {
                Class.forName("kr.heukhyeon.service_locator.RootInjectorImpl")
                    .declaredConstructors
                    .first()
                    .newInstance(context) as RootInjector
            }
            /**
             * 클래스 자체가 생성되지 않은경우
             */
            catch (e: ClassNotFoundException) {
                throw IllegalStateException("RootInjector is Not Generated. Make sure you apply @ApplicationEntryPoint to your Application Class.")
            }
            /**
             * 클래스가 만들어지긴 했는데 생성자가 난독화되서 확인할 수 없는경우
             */
            catch (e : NoSuchElementException) {
                throw IllegalStateException("RootInjector Constructor is corrupted. Please check your Proguard settings.")
            }
            instance = impl
        }
    }
}