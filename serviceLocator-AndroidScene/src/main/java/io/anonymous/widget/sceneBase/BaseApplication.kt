package io.anonymous.widget.sceneBase

import android.app.Application
import kr.heukhyeon.service_locator.Initializer
import kr.heukhyeon.service_locator.RootInjector
import kr.heukhyeon.service_locator.initializer.AndroidInitializer
import kr.heukhyeon.service_locator.provider.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*

abstract class BaseApplication : Application(), AndroidInitializer {

    @ExperimentalCoroutinesApi
    override val proceeded = MutableStateFlow(Initializer.Phase.NOT_INITIALIZE)

    final override val providerBuffer: LinkedList<Provider<*>> = LinkedList()

    final override val parentInitializer: AndroidInitializer
        get() = this

    final override val delayUntilParentStep: Initializer.Phase
        get() = Initializer.Phase.NOT_INITIALIZE

    override fun onCreate() {
        super.onCreate()
        startInitialize()
    }

    override suspend fun initializeInWorkerThread() {
        super.initializeInWorkerThread()
        RootInjector.initialize(this)
    }

    @DelicateCoroutinesApi
    override fun getCoroutineScope(): CoroutineScope {
        return GlobalScope
    }
}