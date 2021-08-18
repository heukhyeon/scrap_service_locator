package kr.heukhyeon.sample

import kr.heukhyeon.service_locator.Component

@Component
class SamplePresenter(
    @SampleQualifier private val sampleRepository: SampleRepository,
    private val sampleRepository2: SampleRepository
    ) {

    fun getTestText(): String {
        return sampleRepository.getTestText()
    }

    suspend fun updateClickedTime() : String {
        return sampleRepository.putLatestClickedTime()
    }
}