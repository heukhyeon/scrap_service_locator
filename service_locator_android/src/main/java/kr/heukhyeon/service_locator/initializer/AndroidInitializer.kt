package kr.heukhyeon.service_locator.initializer

import androidx.annotation.WorkerThread
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kr.heukhyeon.service_locator.Initializer
import kr.heukhyeon.service_locator.provider.Provider
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kr.heukhyeon.service_locator.RootInjector
import kr.heukhyeon.service_locator.lifecycle.InjectLifecycleManager
import java.util.*

interface AndroidInitializer : Initializer {

    /**
     * [InjectLifecycleManager] 을 통해 진행 순서를 제어하므로 기본적으로는 필요없다.
     */
    override val proceeded: MutableStateFlow<Initializer.Phase>?
        get() = null

    val isInitialized get() = InjectLifecycleManager.getInstance().isInitialized(this)

    @WorkerThread
    override suspend fun initializeInWorkerThread() {
        InjectLifecycleManager.getInstance().awaitInitializerReady(this)
        super.initializeInWorkerThread()
    }

    override suspend fun onInitialize() {
        super.onInitialize()
        InjectLifecycleManager.getInstance().onInitialize(this)
    }

    @Suppress("UNCHECKED_CAST")
    fun getViewProviders() : List<Provider<out ViewBinding>> {
        return providerBuffer.mapNotNull {
            if (it.clazz.java.interfaces.contains(ViewBinding::class.java)) {
                return@mapNotNull it as Provider<out ViewBinding>
            }
            return@mapNotNull null
        }
    }
}