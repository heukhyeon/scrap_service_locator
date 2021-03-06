package kr.heukhyeon.sample

import kotlinx.coroutines.delay
import kr.heukhyeon.service_locator.Component
import kr.heukhyeon.service_locator.ComponentQualifier
import kr.heukhyeon.service_locator.ComponentScope

interface SampleRepository {
    fun getTestText(): String
    suspend fun putLatestClickedTime() : String
}

@Component(scope = ComponentScope.IS_SINGLETON, bind = SampleRepository::class)
class SampleRepositoryImpl : SampleRepository {

    private val time = System.currentTimeMillis()

    override fun getTestText(): String {
        val createTime = "Create Time Millis : $time"
        val clickTime = "Latest Click Time : GONE"

        return listOf(createTime, clickTime).joinToString("\n")
    }

    override suspend fun putLatestClickedTime(): String {
        return getTestText()
    }
}

@SampleQualifier
@Component(scope = ComponentScope.IS_SINGLETON, bind = SampleRepository::class)
class SampleRepositoryImpl2 : SampleRepository {

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

@ComponentQualifier
annotation class SampleQualifier