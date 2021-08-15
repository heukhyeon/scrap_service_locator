package kr.heukhyeon.sample

import io.anonymous.widget.sceneBase.BasePresenter
import kr.heukhyeon.service_locator.Component

@Component
class SamplePresenter(
    private val sampleRepository: SampleRepository
) : BasePresenter() {

    fun getTestText(): String {
        return sampleRepository.getTestText()
    }

    suspend fun updateClickedTime() : String {
        return sampleRepository.putLatestClickedTime()
    }
}