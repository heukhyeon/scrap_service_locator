package io.anonymous.module

import android.content.Context
import android.os.Looper
import android.view.LayoutInflater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.heukhyeon.service_locator.ComponentModule
import kr.heukhyeon.service_locator.ComponentOwner
import kr.heukhyeon.service_locator.IComponentModule
import kr.heukhyeon.service_locator.viewbinding.ViewBindingProvider

@ComponentModule
interface AndroidViewBindingModule : IComponentModule {

    suspend fun getViewBindingProvider(owner: ComponentOwner): ViewBindingProvider {
        val realOwner = IComponentModule.SINGLETON_OWNER
        val key = IComponentModule.Key(ViewBindingProvider::class, "")
        return getCachedInstance(realOwner, key) ?: cachingAndReturn(
            realOwner, key,
            ViewBindingProvider(getLayoutInflater(owner))
        )
    }

    suspend fun getLayoutInflater(owner: ComponentOwner): LayoutInflater {
        val realOwner = IComponentModule.SINGLETON_OWNER
        val key = IComponentModule.Key(LayoutInflater::class, "")
        return getCachedInstance(realOwner, key) ?: cachingAndReturn(realOwner, key,
            if (Looper.getMainLooper() == Looper.myLooper()) LayoutInflater.from(getContext(owner))
            else withContext(Dispatchers.Main) {
                LayoutInflater.from(getContext(owner))
            }
        )
    }

    suspend fun getContext(owner: ComponentOwner): Context

}