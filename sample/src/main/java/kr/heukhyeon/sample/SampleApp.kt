package kr.heukhyeon.sample

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kr.heukhyeon.service_locator.ApplicationEntryPoint
import kr.heukhyeon.service_locator.Initializer
import kr.heukhyeon.service_locator.RootInjector
import kr.heukhyeon.service_locator.initializer.AndroidInitializer
import kr.heukhyeon.service_locator.provider.Provider
import java.util.*

@ApplicationEntryPoint
class SampleApp : Application(), AndroidInitializer {

    override val delayUntilParentStep: Initializer.Phase
        get() = Initializer.Phase.NOT_INITIALIZE
    override val parentInitializer: AndroidInitializer
        get() = this

    override val proceeded = MutableStateFlow(Initializer.Phase.NOT_INITIALIZE)

    override val providerBuffer = LinkedList<Provider<*>>()

    override fun getCoroutineScope(): CoroutineScope {
        return GlobalScope
    }


    override fun onCreate() {
        super.onCreate()
        //IMPORTANT
        RootInjector.initialize(this)
        startInitialize()
    }



}