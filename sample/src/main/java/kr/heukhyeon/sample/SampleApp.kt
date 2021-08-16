package kr.heukhyeon.sample

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kr.heukhyeon.service_locator.ApplicationEntryPoint
import kr.heukhyeon.service_locator.Initializer
import kr.heukhyeon.service_locator.RootInjector
import kr.heukhyeon.service_locator.initializer.AndroidInitializer
import kr.heukhyeon.service_locator.lifecycle.InjectLifecycleManager
import kr.heukhyeon.service_locator.provider.Provider
import java.util.*

@ApplicationEntryPoint
class SampleApp : Application(), AndroidInitializer {

    override val providerBuffer = LinkedList<Provider<*>>()

    override fun getCoroutineScope(): CoroutineScope {
        return GlobalScope
    }

    override fun onCreate() {
        super.onCreate()
        //IMPORTANT
        RootInjector.initialize(this)
        // IF USING service_locator_android, MUST CALL
        InjectLifecycleManager.initialize(this)
        startInitialize()
    }

    override suspend fun initializeInWorkerThread() {
        super.initializeInWorkerThread()
        delay(3500L)
    }


}