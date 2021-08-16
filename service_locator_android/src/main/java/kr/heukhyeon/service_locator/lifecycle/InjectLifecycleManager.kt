package kr.heukhyeon.service_locator.lifecycle

import android.app.Activity
import android.app.Application
import androidx.annotation.WorkerThread
import androidx.fragment.app.Fragment
import kr.heukhyeon.service_locator.initializer.AndroidInitializer
import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.HashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * [AndroidInitializer] 를 구현한 클래스들의 [AndroidInitializer.startInitialize] 의존성 주입 작업이
 * Application
 *  ㄴ Activity
 *      ㄴ Fragment
 *   로 이어지는 Android Lifecycle Tree 에 따라 순차적으로 진행될수 있게 한다.
 */
class InjectLifecycleManager(app: Application) {

    class State {
        var isInitialized = false
        val children = LinkedList<Continuation<Unit>>()
    }

    private val stateMap = HashMap<AndroidInitializer, State>()

    private val fragmentManager = FragmentLifecycleManager(stateMap)
    private val activityManager= ActivityLifecycleManager(stateMap, fragmentManager)


    init {
        if (app is AndroidInitializer) {
            stateMap[app] = State()
        }

        app.registerActivityLifecycleCallbacks(activityManager)
    }

    /**
     * 파라미터로 넘어온 객체가 [onInitialize] 가 호출됬는지를 확인한다.
     */
    fun isInitialized(initializer: AndroidInitializer) : Boolean {
        return requireNotNull(stateMap[initializer]).isInitialized
    }

    @WorkerThread
    suspend fun awaitInitializerReady(initializer: AndroidInitializer) {
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