package kr.heukhyeon.sample

import kr.heukhyeon.service_locator.Component

interface SampleRepository {
    fun getTestText(): String
}

@Component(isSingleton = true, bind = SampleRepository::class)
class SampleRepositoryImpl : SampleRepository {

    private val time = System.currentTimeMillis()

    override fun getTestText(): String {
        return "Create Time Millis : $time"
    }
}

