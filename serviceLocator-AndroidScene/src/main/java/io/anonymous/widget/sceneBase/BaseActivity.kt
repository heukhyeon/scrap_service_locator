package io.anonymous.widget.sceneBase

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.FragmentActivity
import kr.heukhyeon.service_locator.Initializer
import kr.heukhyeon.service_locator.initializer.AndroidInitializer
import kr.heukhyeon.service_locator.provider.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

abstract class BaseActivity : FragmentActivity(), AndroidInitializer, CoroutineScope {

    override val proceeded: MutableStateFlow<Initializer.Phase> =
        MutableStateFlow(Initializer.Phase.NOT_INITIALIZE)

    final override val parentInitializer: AndroidInitializer by lazy {
        application as AndroidInitializer
    }

    final override val coroutineContext: CoroutineContext = Dispatchers.Default + Job()

    override val delayUntilParentStep: Initializer.Phase = Initializer.Phase.INITIALIZED_COMPLETE

    final override val providerBuffer: LinkedList<Provider<*>> = LinkedList()

    abstract val presenter: BasePresenter

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val key = AndroidInitializer.KEY_ACTIVITY_UNIQUE_EXTRA
        if (savedInstanceState != null && savedInstanceState.containsKey(key)) {
            intent.putExtra(key, savedInstanceState.getParcelable<Parcelable>(key))
        }
        startInitialize()
    }

    override suspend fun initializeInMainThread() {
        super.initializeInMainThread()
        getViewProviders().also {
            if (it.isEmpty()) return@also
            require(it.size == 1)
            setContentView(it.first().instance?.root)
        }
    }

    abstract override suspend fun onInitialize()

    override fun onDestroy() {
        presenter.onClear()
        dispose()
        super.onDestroy()
    }

    final override fun getCoroutineScope(): CoroutineScope {
        return this
    }

    final override fun <T : Any> inject(clazz: KClass<T>, injectImmediate: Boolean): Provider<T> {
        return super.inject(clazz, injectImmediate)
    }


    companion object {

        fun Intent.putActivityExtra(obj: Parcelable): Intent {
            putExtra(AndroidInitializer.KEY_ACTIVITY_UNIQUE_EXTRA, obj)
            return this
        }
    }
}