package kr.heukhyeon.service_locator.initializer

import android.content.Intent
import android.os.Parcelable
import android.view.View

interface ActivityInitializer : AndroidInitializer {

    /**
     * true (기본값) 일경우 초기화 단계에서 자동으로 현재 액티비티 내의 ViewBinding 을 액티비티에 붙인다.
     * false 일경우 구현 클래스에서 명시적으로 [setContentView] 를 호출해줘야한다.
     */
    val isAutoAttachView : Boolean get() = true

    fun setContentView(view: View)

    override suspend fun initializeInMainThread() {
        super.initializeInMainThread()
        if (isAutoAttachView) {
            getViewProviders().also {
                require(it.isEmpty() || it.size == 1)
                setContentView(it.first().instance!!.root)
            }
        }
    }

    fun putExtraToIntent(intent: Intent, parcelable: Parcelable): Intent {
        intent.putExtra(KEY_ACTIVITY_UNIQUE_EXTRA, parcelable)
        return intent
    }

    companion object {
        const val KEY_ACTIVITY_UNIQUE_EXTRA = "KEY_ACTIVITY_UNIQUE_EXTRA"
    }
}