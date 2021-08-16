package kr.heukhyeon.service_locator.initializer

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kr.heukhyeon.service_locator.Initializer

interface FragmentInitializer : AndroidInitializer {

    /**
     * true (기본값) 일경우, 초기화 단계에서 Fragment 에 정의된 "첫번째" ViewBinding 을 ParentView 에 붙인다.
     * false 로 재정의한경우, 구현 Fragment 가 직접 초기화가 완료된 ViewBinding 을 뷰 계층에 추가해야한다.
     */
    val isAutoAttachView : Boolean get() = true

    fun getView(): View?
    fun getArguments(): Bundle?
    fun setArguments(bundle: Bundle?)

    override suspend fun initializeInMainThread() {
        super.initializeInMainThread()
        if (isAutoAttachView) {
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