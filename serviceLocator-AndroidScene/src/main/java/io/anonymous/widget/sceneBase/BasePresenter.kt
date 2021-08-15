package io.anonymous.widget.sceneBase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

abstract class BasePresenter {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    open fun onClear() {
        coroutineScope.cancel()
    }

    protected fun launch(func: suspend CoroutineScope.() -> Unit) {
        coroutineScope.launch {
            func()
        }
    }
}