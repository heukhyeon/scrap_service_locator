package io.anonymous.module

import kr.heukhyeon.service_locator.*
import kr.heukhyeon.service_locator.recyclerview.ViewHolderParentListener

@ComponentModule
interface ServiceLocatorRecyclerViewModule : IComponentModule {

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : ViewHolderParentListener> getViewHolderParentListener(owner: ComponentOwner): T {
        require(owner is FakeComponentOwner)
        return owner.realComponentOwner as T
    }
}