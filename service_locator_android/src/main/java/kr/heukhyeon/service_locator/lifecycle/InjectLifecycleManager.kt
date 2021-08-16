package kr.heukhyeon.service_locator.lifecycle

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.annotation.WorkerThread
import androidx.fragment.app.Fragment
import kr.heukhyeon.service_locator.initializer.AndroidInitializer
import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.HashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class InjectLifecycleManager(context: Context) {

    class State {
        var isInitialized = false
        val children = LinkedList<Continuation<Unit>>()
    }

    private val stateMap = HashMap<AndroidInitializer, State>()

    private val fragmentManager = FragmentLifecycleManager(stateMap)
    private val activityManager= ActivityLifecycleManager(stateMap, fragmentManager)


    init {
        require(context is Application)
        if (context is AndroidInitializer) {
            stateMap[context] = State()
        }

        context.registerActivityLifecycleCallbacks(activityManager)
    }

    @WorkerThread
    suspend fun awaitInitializerReady(initializer: AndroidInitializer) {
        if (initializer !is Activity) return
        val parent = getParentInitializer(initializer) ?: return
        val state = requireNotNull(stateMap[parent])

        if (state.isInitialized) return

        return suspendCoroutine {
            state.children.add(it)
        }
    }

    @WorkerThread
    fun onInitialize(initializer: AndroidInitializer) {
        requireNotNull(stateMap[initializer]).also { state ->
            state.isInitialized = true
            state.children.forEach { it.resume(Unit) }
        }
    }

    /**
     * Application -> Must Null
     * Activity -> Application
     * Fragment -> Parent Fragment or Activity
     * if return null, Android 구조상 상위 컴포넌트인 클래스가 [AndroidInitializer] 를 미구현한 경우
     */
    private fun getParentInitializer(initializer: AndroidInitializer) : AndroidInitializer? {
        return when (initializer) {
            is Activity -> initializer.application as? AndroidInitializer
            is Fragment -> initializer.parentFragment as? AndroidInitializer ?: initializer.activity as? AndroidInitializer
            is Application -> null
            else -> throw IllegalStateException("AndroidInitializer is Only Supported Activity, Fragment, Application")
        }
    }

    companion object {
        private var mInstance : InjectLifecycleManager? = null

        fun initialize(context: Application) {
            mInstance = InjectLifecycleManager(context)
        }

        internal fun getInstance() : InjectLifecycleManager {
           return requireNotNull(mInstance)
        }
    }
}