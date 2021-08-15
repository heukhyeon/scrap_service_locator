package io.anonymous.widget.sceneBase.asyncInflate

import androidx.recyclerview.widget.RecyclerView

open class AsyncViewHolder(private val asyncFrameLayout: AsyncFrameLayout) :
    RecyclerView.ViewHolder(asyncFrameLayout) {

    fun bindAsync(runnable: () -> Unit) {
        asyncFrameLayout.enqueueRunnable(runnable)
    }
}