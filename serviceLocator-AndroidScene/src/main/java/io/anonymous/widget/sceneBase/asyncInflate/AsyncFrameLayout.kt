package io.anonymous.widget.sceneBase.asyncInflate

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewParent
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import kr.heukhyeon.service_locator.Initializer
import kr.heukhyeon.service_locator.initializer.AndroidInitializer
import kr.heukhyeon.service_locator.provider.Provider
import io.anonymous.widget.util.requireIsWorkerThread
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import java.util.*
import kotlin.coroutines.CoroutineContext

abstract class AsyncFrameLayout(context: Context, attributeSet: AttributeSet?) :
    FrameLayout(context, attributeSet), AndroidInitializer, CoroutineScope {

    final override val proceeded: MutableStateFlow<Initializer.Phase> =
        MutableStateFlow(Initializer.Phase.NOT_INITIALIZE)

    override val coroutineContext: CoroutineContext = Dispatchers.Default + Job()

    private var mParentInitializer : AndroidInitializer? = null

    final override val parentInitializer: AndroidInitializer
    get() {
        if (mParentInitializer != null) return mParentInitializer!!
        require(isAttachedToWindow)
        var viewParent: ViewParent? = this
        /**
         * 뷰 계층에 Initialzier가 있는지 체크
         */
        while (viewParent != null) {
            viewParent = viewParent.parent
            if (viewParent is AndroidInitializer) return viewParent
        }

        mParentInitializer = try {
            throw IllegalStateException()
//            FragmentManager.findFragment<BaseFragment>(this)
        } catch (e: IllegalStateException) {
            when (val context = context) {
                is ContextThemeWrapper -> context.baseContext as AndroidInitializer
                else -> context as AndroidInitializer
            }
        }

        return mParentInitializer!!
    }

    final override val delayUntilParentStep: Initializer.Phase =
        Initializer.Phase.INITIALIZED_MAIN_THREAD

    override val providerBuffer = LinkedList<Provider<*>>()

    private var childView: View? = null
    private val bindFlow = MutableStateFlow {}
    var isInitialized = false
        private set

    override suspend fun onInitialize() {
        isInitialized = true
        getCoroutineScope().launch {
            bindFlow.collect { runnable ->
                launch(Dispatchers.Main) {
                    runnable.invoke()
                }
            }
        }
    }

    fun enqueueRunnable(runnable: () -> Unit) = runBlocking {
        bindFlow.emit(runnable)
    }

    override suspend fun initializeInMainThread() {
        super.initializeInMainThread()
        val view = getViewProviders().first().instance?.root
        addView(view)
        childView = view
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startInitialize()
    }

    override fun startInitialize() {
        require(getViewProviders().size == 1)
        super.startInitialize()
        initDisposeTimer()
    }

    override fun dispose() {
        childView = null
        mParentInitializer = null
        isInitialized = false
        super.dispose()
    }

    override fun onDraw(canvas: Canvas?) {
        childView?.draw(canvas) ?: super.onDraw(canvas)
    }

    /**
     * 일반적으로는 [onDetachedFromWindow] 로 뷰가 Dispose 됬다 말할수있지만, RecyclerView 는
     * View 를 붙였다 뗏다를 자주하므로 자기 자신이 아닌 RecyclerView에 대한 [onDetachedFromWindow]를 관측하는게 더 맞을수도 있다.
     * 어느쪽이건 택할수있어야하므로 오버라이드 가능하게 만든다.
     *
     * 기본 구현은 RecyclerView 에 대한 관측이다.
     */
    open fun initDisposeTimer() {
        requireNotNull(parent) { "expected View is Attached Window, But ViewParent is Null" }
        var parentView : ViewParent = this
        while (parentView.parent != null) {
            parentView = parentView.parent
            if (parentView is RecyclerView) {
                parentView.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View?) = Unit


                    override fun onViewDetachedFromWindow(v: View?) {
                        v?.removeOnAttachStateChangeListener(this)
                        dispose()
                    }
                })
                break
            }
        }
    }

    override fun getCoroutineScope(): CoroutineScope {
        return this
    }
}