package kr.heukhyeon.service_locator.initializer

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kr.heukhyeon.service_locator.Initializer

interface FragmentInitializer : AndroidInitializer {

    override val parentInitializer: AndroidInitializer
        get() {
            val parentFragment = getParentFragment()
            val activity = getActivity()
            require(parentFragment != null || activity != null)

            if (parentFragment != null) {
                require(parentFragment is AndroidInitializer)
                return parentFragment
            }
            require(activity is AndroidInitializer)
            return activity
        }

    override val delayUntilParentStep: Initializer.Phase
        get() = Initializer.Phase.INITIALIZED_COMPLETE

    fun getView(): View?
    fun getActivity(): Activity?
    fun getParentFragment(): Fragment?
    fun getArguments(): Bundle?
    fun setArguments(bundle: Bundle?)

    override suspend fun initializeInMainThread() {
        super.initializeInMainThread()
        getViewProviders().also { buffer ->
            if (buffer.isNotEmpty()) {
                val dummyView = getView()
                val root = buffer[0].instance!!.root
                val viewParent = dummyView?.parent
                require(viewParent is ViewGroup)
                val index = viewParent.indexOfChild(dummyView)
                viewParent.removeViewAt(index)
                viewParent.addView(root, index, dummyView.layoutParams)
            }
        }
    }

    /**
     * 단일 Extra Key 로 조회하므로 한 Fragment 에서는 한번만 호출되어야한다.
     */
    fun putExtra(parcelable: Parcelable) {
        val arguments = getArguments()
        val key = KEY_FRAGMENT_UNIQUE_EXTRA
        require(arguments?.containsKey(key) != true)
        if (arguments == null) {
            val bundle = Bundle()
            bundle.putParcelable(key, parcelable)
            setArguments(bundle)
        } else {
            arguments.putParcelable(key, parcelable)
        }
    }

    companion object {
        const val KEY_FRAGMENT_UNIQUE_EXTRA = "KEY_FRAGMENT_UNIQUE_EXTRA"
    }
}