package io.anonymous.widget.sceneBase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kr.heukhyeon.service_locator.Initializer
import kr.heukhyeon.service_locator.initializer.FragmentInitializer
import kr.heukhyeon.service_locator.provider.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

abstract class BaseFragment : Fragment(), FragmentInitializer, CoroutineScope  {

    interface ParentListener

    final override val providerBuffer = LinkedList<Provider<*>>()

    final override val proceeded: MutableStateFlow<Initializer.Phase> =
        MutableStateFlow(Initializer.Phase.NOT_INITIALIZE)

    final override val coroutineContext: CoroutineContext = Dispatchers.Default + Job()

    abstract val presenter: BasePresenter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return if (container == null) null
        else View(container.context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view.isAttachedToWindow) {
            startInitialize()
        }
    }

    final override fun getView(): View? {
        val view = getViewProviders().firstOrNull()?.instance?.root
        return when (view?.parent) {
            null -> super.getView()
            else -> view
        }
    }

    abstract override suspend fun onInitialize()

    override fun onDestroyView() {
        if (proceeded.value == Initializer.Phase.INITIALIZED_COMPLETE) {
            presenter.onClear()
        }
        dispose()
        super.onDestroyView()
    }

    final override fun getCoroutineScope(): CoroutineScope {
        return this
    }

    final override fun <T : Any> inject(clazz: KClass<T>, injectImmediate: Boolean): Provider<T> {
        return super.inject(clazz, injectImmediate)
    }
}