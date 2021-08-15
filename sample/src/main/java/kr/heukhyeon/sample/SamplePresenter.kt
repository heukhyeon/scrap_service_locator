package kr.heukhyeon.sample

import kr.heukhyeon.service_locator.Component

@Component
class SamplePresenter(
    private val sampleRepository: SampleRepository
) {

    fun getTestText(): String {
        return sampleRepository.getTestText()
    }
}