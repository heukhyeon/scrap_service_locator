package kr.heukhyeon.service_locator.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import kr.heukhyeon.service_locator.initializer.AndroidInitializer

/**
 * 현재 Intent 에 수신자가 inject 로 주입받을수있는 [Parcelable] 객체를 추가한다.
 */
fun Intent.putExtraToIntent(extra : Parcelable) : Intent {
    putExtra(ServiceLocatorFragmentKeys.KEY_ACTIVITY_UNIQUE_EXTRA, extra)
    return this
}

/**
 * 현재 Fragment 에 Fragment 스스로 inject 로 주입받을수있는 [Parcelable] 객체를 추가한다.
 */
fun<T> T.putExtra(extra: Parcelable) : T where T : Fragment, T: AndroidInitializer {
    if (arguments == null) {
        val bundle = Bundle()
        bundle.putParcelable(ServiceLocatorFragmentKeys.KEY_FRAGMENT_UNIQUE_EXTRA, extra)
        arguments = bundle
    }
    else {
        arguments?.putParcelable(ServiceLocatorFragmentKeys.KEY_FRAGMENT_UNIQUE_EXTRA, extra)
    }
    return this
}