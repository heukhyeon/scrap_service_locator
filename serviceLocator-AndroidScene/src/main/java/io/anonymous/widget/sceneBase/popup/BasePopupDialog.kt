package io.anonymous.widget.sceneBase.popup

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import kr.heukhyeon.service_locator.FragmentParentListener
import kr.heukhyeon.service_locator.Initializer
import kr.heukhyeon.service_locator.initializer.FragmentInitializer
import kr.heukhyeon.service_locator.provider.Provider
import io.anonymous.widget.sceneBase.BaseFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kr.heukhyeon.service_locator.EntryPoint
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

@EntryPoint
abstract class BasePopupDialog : DialogFragment(), FragmentInitializer, CoroutineScope {

    final override val proceeded: MutableStateFlow<Initializer.Phase> =
        MutableStateFlow(Initializer.Phase.NOT_INITIALIZE)

    interface ResultContract : FragmentParentListener {
        fun onPopupResult(popupDialog: BasePopupDialog, tag: String, result: Bundle)
    }

    companion object {
        internal const val BUNDLE_KEY_RESULT_OK = "BUNDLE_KEY_RESULT_OK"
    }

    final override val providerBuffer = LinkedList<Provider<*>>()

    final override val coroutineContext: CoroutineContext = Dispatchers.Default + Job()

    private val owner: ResultContract by inject(ResultContract::class)


    val isResultOk: Boolean
        get() = resultBundle.getBoolean(BUNDLE_KEY_RESULT_OK, false)

    private lateinit var resultBundle: Bundle

    final override fun getView(): View? {
        val view = getViewProviders().firstOrNull()?.instance?.root
        return when (view?.parent) {
            null -> super.getView()
            else -> view
        }
    }

    override suspend fun initializeInWorkerThread() {
        super.initializeInWorkerThread()
        resultBundle = Bundle()
    }

    override suspend fun initializeInMainThread() {
        super.initializeInMainThread()
        getViewProviders().forEachIndexed { index, injectProviderByRootInjector ->
            if (index == 0) dialog?.setContentView(injectProviderByRootInjector.instance!!.root)
            else applyViewHierarchy(injectProviderByRootInjector.instance!!.root)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BackPressObserveDialog(requireContext(), theme).apply {
            onBackPressedListener = this@BasePopupDialog::onBackPressed
            setOnShowListener {
                startInitialize()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        owner.onPopupResult(this@BasePopupDialog, tag ?: "", resultBundle)
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        dispose()
        super.onDestroyView()
    }

    open fun onBackPressed() = true

    abstract override suspend fun onInitialize()

    protected fun createResultOkBundle(): Bundle {
        val bundle = Bundle()
        bundle.putBoolean(BUNDLE_KEY_RESULT_OK, true)
        return bundle
    }

    protected fun setResultAndDismiss(result: Bundle) {
        this.resultBundle = result
        dismiss()
    }

    /**
     * 하나의 Dialog 에 두개 이상의 뷰바인딩이 붙을때 호출됨.
     * 첫번째 뷰는 무조건 [Dialog.setContentView]에 사용되므로 이 함수의 대상이 되지 않는다.
     */
    protected open fun applyViewHierarchy(view: View) {
        throw IllegalStateException("If You Use Over 1 ViewBindings, Must Override applyViewHierarchy")
    }

    final override fun getCoroutineScope(): CoroutineScope {
        return this
    }

    final override fun <T : Any> inject(clazz: KClass<T>, injectImmediate: Boolean): Provider<T> {
        return super.inject(clazz, injectImmediate)
    }
}