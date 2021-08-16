package kr.heukhyeon.service_locator.initializer

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

interface FragmentInitializer : AndroidInitializer {

    /**
     * [Fragment.getView] 는 재정의 가능한 함수지만, 안드로이드는 [Fragment.getView] 가 [Fragment.onCreateView] 때 생성한 뷰와 다르다 해도
     * [Fragment.mView] 를 업데이트하지 않으며, [Fragment.mView] 는 [androidx.fragment.app.FragmentTransaction] 에서 활용된다.
     *
     * 그러므로 [getView] 를 오버라이드해 [getViewProviders] 에서 만들어진 뷰를 리턴하는경우
     * [androidx.fragment.app.FragmentTransaction] 의 작동이 예상한대로 이루어지지 않을수 있다.
     *
     * 이때문에 기본값은 false 이며, true 라 해도 기존의 [getView] 를 뷰 계층에서 제거하지 않고, [getView] 의 하위 계층에 추가한다.
     */
    val isAutoAttachView : Boolean get() = false

    fun getView(): View?
    fun getArguments(): Bundle?
    fun setArguments(bundle: Bundle?)

    override suspend fun initializeInMainThread() {
        super.initializeInMainThread()
        val existingView = getView()

        if (isAutoAttachView && existingView is ViewGroup) {
            getViewProviders().also { buffer ->
                if (buffer.isNotEmpty()) {
                    val root = buffer[0].instance!!.root
                    existingView.addView(root)
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