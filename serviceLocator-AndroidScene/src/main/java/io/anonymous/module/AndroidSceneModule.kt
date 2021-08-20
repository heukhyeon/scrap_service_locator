package io.anonymous.module

import io.anonymous.widget.sceneBase.popup.BasePopupDialog
import kr.heukhyeon.service_locator.ComponentModule
import kr.heukhyeon.service_locator.ComponentOwner
import kr.heukhyeon.service_locator.fragment.FragmentParentListener
import kr.heukhyeon.service_locator.IComponentModule

@ComponentModule
interface AndroidSceneModule : IComponentModule {

    suspend fun getBasePopupDialogResultContract(owner: ComponentOwner) : BasePopupDialog.ResultContract {
        return getFragmentParentListener(owner)
    }

    suspend fun <T : FragmentParentListener> getFragmentParentListener(owner: ComponentOwner) : T

}