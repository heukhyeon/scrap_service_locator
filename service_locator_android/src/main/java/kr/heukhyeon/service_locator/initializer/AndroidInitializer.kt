package kr.heukhyeon.service_locator.initializer

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.MutableStateFlow
import kr.heukhyeon.service_locator.Initializer
import kr.heukhyeon.service_locator.lifecycle.InjectLifecycleManager

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
}