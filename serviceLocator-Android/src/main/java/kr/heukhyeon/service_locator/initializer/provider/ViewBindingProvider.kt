package kr.heukhyeon.service_locator.initializer.provider

import android.app.Application
import android.content.Context
import android.os.Looper
import android.view.View
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ViewBindingProvider(private val context: Context) {

    init {
        require(context is Application)
    }

    suspend fun<T : ViewBinding> create(layoutId: Int, factory: (View)-> T) : T {
        require(Looper.getMainLooper() != Looper.myLooper())
        val inflater = withContext(Dispatchers.Main) {
            AsyncLayoutInflater(context)
        }
        val view = suspendCoroutine<View> {
            inflater.inflate(layoutId, null) { view, _, _ ->
                it.resume(view)
            }
        }
        require(Looper.getMainLooper() != Looper.myLooper())
        return factory(view)
    }
}