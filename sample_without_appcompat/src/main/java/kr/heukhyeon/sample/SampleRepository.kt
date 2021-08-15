package kr.heukhyeon.sample

import kotlinx.coroutines.delay
import kr.heukhyeon.service_locator.Component

interface SampleRepository {
    fun getTestText(): String
    suspend fun putLatestClickedTime() : String
}

@Component(isSingleton = true, bind = SampleRepository::class)
class SampleRepositoryImpl : SampleRepository {

    private val time = System.currentTimeMillis()
    private var latestClickedTime : Long? = null

    override fun getTestText(): String {
        val createTime = "Create Time Millis : $time"
        val clickTime = "Latest Click Time : ${if(latestClickedTime == null) "NULL" else latestClickedTime}"

        return listOf(createTime, clickTime).joinToString("\n")
    }

    override suspend fun putLatestClickedTime(): String {
        delay(1000L)
        latestClickedTime = System.currentTimeMillis()
        return getTestText()
    }
}

