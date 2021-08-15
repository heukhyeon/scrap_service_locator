package io.anonymous.module

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.os.Parcelable
import androidx.fragment.app.Fragment
import kr.heukhyeon.service_locator.ComponentModule
import kr.heukhyeon.service_locator.ComponentOwner
import kr.heukhyeon.service_locator.IComponentModule
import kr.heukhyeon.service_locator.initializer.AndroidInitializer
import kr.heukhyeon.service_locator.initializer.FragmentInitializer
import kr.heukhyeon.service_locator.FragmentParentListener
import kr.heukhyeon.service_locator.initializer.provider.ViewBindingProvider
import java.lang.IllegalStateException

@ComponentModule
interface AndroidDependencyModule : IComponentModule {

    suspend fun <T : Parcelable> getParcelable(owner: ComponentOwner) : T {
        val (bundle, key) = when (owner) {
            is Fragment -> owner.arguments to FragmentInitializer.KEY_FRAGMENT_UNIQUE_EXTRA
            is Activity -> owner.intent.extras to AndroidInitializer.KEY_ACTIVITY_UNIQUE_EXTRA
            else -> throw IllegalStateException("$owner is Must Fragment or Activity")
        }
        return requireNotNull(bundle?.getParcelable(key))
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : FragmentParentListener> getFragmentParentListener(owner: ComponentOwner) : T {
        require(owner is Fragment)
        return owner.parentFragment as? T ?: owner.activity as? T ?: throw IllegalStateException()
    }

    suspend fun getViewBindingProvider(owner: ComponentOwner): ViewBindingProvider {
        return cachingAndReturn(IComponentModule.SINGLETON_OWNER, ViewBindingProvider::class) {
            ViewBindingProvider(getContext(owner))
        }
    }

    suspend fun getResources(owner: ComponentOwner): Resources {
        return cachingAndReturn(IComponentModule.SINGLETON_OWNER, Resources::class) {
            getContext(owner).resources
        }
    }

    suspend fun getContext(owner: ComponentOwner) : Context

}