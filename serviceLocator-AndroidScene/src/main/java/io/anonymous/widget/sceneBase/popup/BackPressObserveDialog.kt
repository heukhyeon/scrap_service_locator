package io.anonymous.widget.sceneBase.popup

import android.app.Dialog
import android.content.Context

class BackPressObserveDialog(context: Context, themeId: Int) : Dialog(context, themeId) {

    /**
     * 결과값이 true 인경우 기존 처리 (dismiss 를 실행한다)
     */
    var onBackPressedListener: (() -> Boolean)? = null

    override fun onBackPressed() {
        if (onBackPressedListener?.invoke() != false) super.onBackPressed()
    }

    override fun dismiss() {
        onBackPressedListener = null
        super.dismiss()
    }
}