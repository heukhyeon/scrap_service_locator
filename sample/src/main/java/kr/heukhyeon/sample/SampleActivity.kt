package kr.heukhyeon.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kr.heukhyeon.sample.databinding.ActivitySampleBinding
import kr.heukhyeon.service_locator.EntryPoint
import kr.heukhyeon.service_locator.Initializer
import kr.heukhyeon.service_locator.initializer.AndroidInitializer
import kr.heukhyeon.service_locator.provider.Provider
import java.util.*

@EntryPoint
class SampleActivity : AppCompatActivity(), AndroidInitializer {

    override val delayUntilParentStep: Initializer.Phase
        get() = Initializer.Phase.INITIALIZED_COMPLETE
    override val parentInitializer: AndroidInitializer
        get() = application as AndroidInitializer

    override val providerBuffer: LinkedList<Provider<*>> = LinkedList()

    override val proceeded = MutableStateFlow(Initializer.Phase.NOT_INITIALIZE)

    private val presenter by inject(SamplePresenter::class)
    private val binding by inject(ActivitySampleBinding::class)

    override fun getCoroutineScope(): CoroutineScope {
        return lifecycleScope
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startInitialize()
    }

    override suspend fun initializeInMainThread() {
        super.initializeInMainThread()
        setContentView(binding.root)
    }

    override suspend fun onInitialize() {
        super.onInitialize()
        withContext(Dispatchers.Main) {
            binding.textView.text = presenter.getTestText()
        }
    }

}