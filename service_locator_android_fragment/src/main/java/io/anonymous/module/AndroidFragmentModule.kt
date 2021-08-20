package io.anonymous.module

import androidx.fragment.app.Fragment
import kr.heukhyeon.service_locator.ComponentModule
import kr.heukhyeon.service_locator.ComponentOwner
import kr.heukhyeon.service_locator.FakeComponentOwner
import kr.heukhyeon.service_locator.IComponentModule
import kr.heukhyeon.service_locator.fragment.FragmentParentListener

@ComponentModule
interface AndroidFragmentModule : IComponentModule {

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : FragmentParentListener> getFragmentParentListener(owner: ComponentOwner): T {
        val realOwner = if (owner is FakeComponentOwner) owner.realComponentOwner else owner
        require(realOwner is Fragment)
        return realOwner.parentFragment as? T ?: realOwner.activity as? T
        ?: throw IllegalStateException()
    }
}