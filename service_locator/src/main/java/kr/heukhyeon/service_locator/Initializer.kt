package kr.heukhyeon.service_locator

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import java.util.*

interface Initializer : ComponentOwner {

    enum class Phase(private val step: Int) {
        NOT_INITIALIZE(0),
        INITIALIZE_START(1),
        INITIALIZED_WORKER_THREAD(2),
        INITIALIZED_MAIN_THREAD(3),
        INITIALIZED_COMPLETE(4);

        fun isCompleted(expectedPhase: Phase): Boolean {
            return step >= expectedPhase.step
        }
    }

    /**
     * 현재 초기화 단계를 알고싶을경우, 반드시 고정 객체로 초기화해야한다.
     */
    val proceeded: MutableStateFlow<Phase>?

    fun startInitialize() {
        getCoroutineScope().launch {

            if (proceeded != null) require(proceeded === proceeded)

            if (proceeded?.value == Phase.INITIALIZED_COMPLETE) return@launch

            proceeded?.emit(Phase.INITIALIZE_START)
            withContext(Dispatchers.Default) { initializeInWorkerThread() }
            proceeded?.emit(Phase.INITIALIZED_WORKER_THREAD)
            withContext(Dispatchers.Main) { initializeInMainThread() }
            proceeded?.emit(Phase.INITIALIZED_MAIN_THREAD)
            onInitialize()
            proceeded?.emit(Phase.INITIALIZED_COMPLETE)
        }
    }

    suspend fun initializeInWorkerThread() {
        providerInit()
    }

    suspend fun initializeInMainThread() {

    }

    suspend fun onInitialize() = Unit

    override fun dispose() {
        getCoroutineScope().coroutineContext.cancelChildren()
        super.dispose()
    }

    fun getCoroutineScope() : CoroutineScope
}