package kr.heukhyeon.service_locator.lifecycle

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import kr.heukhyeon.service_locator.initializer.AndroidInitializer
import java.util.*
import kotlin.collections.HashMap

internal class FragmentLifecycleManager(
    private val parentMap: HashMap<AndroidInitializer, InjectLifecycleManager.State>,
) : FragmentManager.FragmentLifecycleCallbacks() {

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        super.onFragmentCreated(fm, f, savedInstanceState)
        if (f !is AndroidInitializer) return
        require(parentMap.containsKey(f).not())
        parentMap[f] = InjectLifecycleManager.State()
    }

    /**
     * Fragment 는 Fragment 객체 자체는 Lifecycle 상 Destroy 가 아니더라도 뷰는 파괴됬을수 있기에,
     * 뷰에 대한 초기화 작업을 다시 해줘야할수 있다.
     *
     * 이때문에 [InjectLifecycleManager.State] 객체 자체는 Created - Destroy 의 Lifecycle 을 가지지만,
     * [InjectLifecycleManager.State.isInitialized] 는 Fragment Lifecycle 내에서 한번 이상 토글될수있다.
     */
    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?
    ) {
        super.onFragmentViewCreated(fm, f, v, savedInstanceState)
        if (f !is AndroidInitializer) return
        f.startInitialize()
    }

    override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
        super.onFragmentViewDestroyed(fm, f)
        if (f !is AndroidInitializer) return
        requireNotNull(parentMap[f]).isInitialized = false
        f.dispose()
    }

    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        super.onFragmentDestroyed(fm, f)
        if (f !is AndroidInitializer) return
        parentMap.remove(f)
    }
}