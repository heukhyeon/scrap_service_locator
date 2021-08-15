package kr.heukhyeon.service_locator.initializer

import androidx.annotation.WorkerThread
import androidx.viewbinding.ViewBinding
import kr.heukhyeon.service_locator.Initializer
import kr.heukhyeon.service_locator.provider.Provider
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import java.util.*

interface AndroidInitializer : Initializer {

    val parentInitializer: AndroidInitializer
    val delayUntilParentStep: Initializer.Phase

    @WorkerThread
    override suspend fun initializeInWorkerThread() {
        parentInitializer.proceeded.filter { it.isCompleted(delayUntilParentStep) }.first()
        super.initializeInWorkerThread()
    }


    @Suppress("UNCHECKED_CAST")
    fun getViewProviders() : List<Provider<out ViewBinding>> {
        return providerBuffer?.mapNotNull {
            if (it.clazz.java.interfaces.contains(ViewBinding::class.java)) {
                return@mapNotNull it as Provider<out ViewBinding>
            }
            return@mapNotNull null
        } ?: emptyList()
    }

    companion object {
        const val KEY_ACTIVITY_UNIQUE_EXTRA = "KEY_ACTIVITY_UNIQUE_EXTRA"
    }
}