package kr.heukhyeon.sample

import android.os.Bundle
import android.view.View
import io.anonymous.widget.sceneBase.BaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.heukhyeon.sample.databinding.ActivitySampleBinding
import kr.heukhyeon.service_locator.EntryPoint
import kr.heukhyeon.service_locator.Initializer
import kr.heukhyeon.service_locator.initializer.AndroidInitializer
import kr.heukhyeon.service_locator.provider.Provider
import java.util.*

@EntryPoint
class SampleActivity : BaseActivity() {

    override val presenter by inject(SamplePresenter::class)
    private val binding by inject(ActivitySampleBinding::class)

    override suspend fun initializeInMainThread() {
        super.initializeInMainThread()
        setContentView(binding.root)
    }

    override suspend fun onInitialize() {
        withContext(Dispatchers.Main) {
            binding.textView.text = presenter.getTestText()
            binding.updateButtonView.setOnClickListener {
                binding.updateButtonView.isEnabled = false
                binding.loadingView.visibility = View.VISIBLE
                getCoroutineScope().launch {
                    updateTime()
                }
            }
        }
    }

    private suspend fun updateTime() {
        val updatedText = presenter.updateClickedTime()
        withContext(Dispatchers.Main) {
            binding.updateButtonView.isEnabled = true
            binding.loadingView.visibility = View.GONE
            binding.textView.text = updatedText
        }
    }

}