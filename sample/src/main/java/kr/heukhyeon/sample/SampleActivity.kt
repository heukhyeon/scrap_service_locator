package kr.heukhyeon.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.heukhyeon.sample.databinding.ActivitySampleBinding
import kr.heukhyeon.service_locator.EntryPoint
import kr.heukhyeon.service_locator.Initializer
import kr.heukhyeon.service_locator.initializer.AndroidInitializer
import kr.heukhyeon.service_locator.provider.IProvider
import java.util.*

@EntryPoint
class SampleActivity : AppCompatActivity(), AndroidInitializer {

    override val providerBuffer: LinkedList<IProvider<*>> = LinkedList()

    override val proceeded = MutableStateFlow(Initializer.Phase.NOT_INITIALIZE)

    private val presenter by inject(SamplePresenter::class)
    private val binding by inject(ActivitySampleBinding::class)

    override fun getCoroutineScope(): CoroutineScope {
        return lifecycleScope
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_loading)
    }

    override suspend fun onInitialize() {
        super.onInitialize()
        withContext(Dispatchers.Main) {
            setContentView(binding.root)
            binding.textView.text = presenter.getTestText()
            binding.viewSwitch.setOnCheckedChangeListener { _, isChecked ->
                binding.textView.text = presenter.updateChecked(isChecked)
            }
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