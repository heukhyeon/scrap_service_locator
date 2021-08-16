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

    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?
    ) {
        super.onFragmentViewCreated(fm, f, v, savedInstanceState)
        if (f !is AndroidInitializer) return
        require(parentMap.containsKey(f).not())

        parentMap[f] = InjectLifecycleManager.State()
        f.startInitialize()
    }

    override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
        super.onFragmentViewDestroyed(fm, f)
        if (f !is AndroidInitializer) return
        parentMap.remove(f)
        f.dispose()
    }
}