package kr.heukhyeon.service_locator.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import kr.heukhyeon.service_locator.initializer.AndroidInitializer
import kotlin.collections.HashMap

internal class ActivityLifecycleManager(
    private val parentMap: HashMap<AndroidInitializer, InjectLifecycleManager.State>,
    private val fragmentLifecycleManager: FragmentLifecycleManager
) : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity !is AndroidInitializer || activity !is FragmentActivity) return
        require(parentMap.containsKey(activity).not())
        parentMap[activity] = InjectLifecycleManager.State()
        fragmentLifecycleManager.onCreateActivity(activity)
        activity.startInitialize()
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity !is AndroidInitializer || activity !is FragmentActivity) return
        /**
         * 이게 주석인 이유는 [FragmentLifecycleManager.onCreateActivity] 의 주석 참고
         */
        // activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleManager)
        activity.dispose()
        parentMap.remove(activity)
    }

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}