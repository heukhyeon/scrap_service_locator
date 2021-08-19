package io.anonymous.module

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.os.Parcelable
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.heukhyeon.service_locator.*
import kr.heukhyeon.service_locator.initializer.FragmentInitializer
import kr.heukhyeon.service_locator.initializer.ActivityInitializer
import kr.heukhyeon.service_locator.initializer.provider.ViewBindingProvider
import java.lang.IllegalStateException

@ComponentModule
interface AndroidDependencyModule : IComponentModule {

    suspend fun <T : Parcelable> getParcelable(owner: ComponentOwner) : T {
        val (bundle, key) = when (owner) {
            is Fragment -> owner.arguments to FragmentInitializer.KEY_FRAGMENT_UNIQUE_EXTRA
            is Activity -> owner.intent.extras to ActivityInitializer.KEY_ACTIVITY_UNIQUE_EXTRA
            else -> throw IllegalStateException("$owner is Must Fragment or Activity")
        }
        return requireNotNull(bundle?.getParcelable(key))
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : FragmentParentListener> getFragmentParentListener(owner: ComponentOwner) : T {
        val realOwner = if (owner is FakeComponentOwner) owner.realComponentOwner else owner
        require(realOwner is Fragment)
        return realOwner.parentFragment as? T ?: realOwner.activity as? T ?: throw IllegalStateException()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : ViewHolderParentListener> getViewHolderParentListener(owner: ComponentOwner) : T {
        require(owner is FakeComponentOwner)
        return owner.realComponentOwner as T
    }

    suspend fun getViewBindingProvider(owner: ComponentOwner): ViewBindingProvider {
        val realOwner = IComponentModule.SINGLETON_OWNER
        val key = IComponentModule.Key(ViewBindingProvider::class, "")
        return getCachedInstance(realOwner, key) ?: cachingAndReturn(realOwner, key,
            ViewBindingProvider(getLayoutInflater(owner))
        )
    }

    suspend fun getLayoutInflater(owner: ComponentOwner) : LayoutInflater {
        val realOwner = IComponentModule.SINGLETON_OWNER
        val key = IComponentModule.Key(LayoutInflater::class, "")
        return getCachedInstance(realOwner, key) ?: cachingAndReturn(realOwner, key,
                if (Looper.getMainLooper() == Looper.myLooper()) LayoutInflater.from(getContext(owner))
                else withContext(Dispatchers.Main) {
                    LayoutInflater.from(getContext(owner))
                }
        )
    }

    suspend fun getContext(owner: ComponentOwner) : Context

}