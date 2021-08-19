package kr.heukhyeon.sample

import kr.heukhyeon.service_locator.Component

@Component
class SamplePresenter(
    private val sampleRepository: SampleRepository,
    @SampleQualifier private val sampleRepository2: SampleRepository
) {

    private var isUseQualifier = false

    fun updateChecked(checked:Boolean): String {
        isUseQualifier = checked
        return getTestText()
    }

    fun getTestText(): String {
         return if (isUseQualifier) sampleRepository2.getTestText()
        else sampleRepository.getTestText()
    }

    suspend fun updateClickedTime(): String {
        sampleRepository2.putLatestClickedTime()
        return getTestText()
    }
}