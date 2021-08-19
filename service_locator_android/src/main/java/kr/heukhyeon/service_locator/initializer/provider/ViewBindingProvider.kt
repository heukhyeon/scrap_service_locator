package kr.heukhyeon.service_locator.initializer.provider

import android.app.Application
import android.content.Context
import android.os.Looper
import android.view.LayoutInflater
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
        val inflater = withContext(Dispatchers.Main) {
            LayoutInflater.from(context)
        }
        val view = inflater.inflate(layoutId, null)
        return factory(view)
    }
}