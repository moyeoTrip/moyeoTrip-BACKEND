package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.config.properties.TourApiProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class LegalDongCodeStartupSync(
    private val properties: TourApiProperties,
    private val legalDongCodeSyncService: LegalDongCodeSyncService,
    private val classificationCodeSyncService: TourClassificationCodeSyncService,
    private val tourismContentSyncService: TourismContentSyncService,
    private val managedTravelCourseSyncService: ManagedTravelCourseSyncService,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        if (!properties.syncOnStartup) return
        syncIfApiKeyExists()
    }

    @Scheduled(cron = "0 0 3 1 * *", zone = "Asia/Seoul")
    fun syncMonthly() {
        syncIfApiKeyExists()
    }

    private fun syncIfApiKeyExists() {
        if (properties.tourApiKey.isBlank()) {
            logger.warn("TOUR_API_KEY가 비어 있어 경상북도 법정동 코드 동기화를 건너뜁니다.")
            return
        }
        val legalDongCount = legalDongCodeSyncService.syncGyeongsangbukdo()
        logger.info("경상북도 법정동 코드 {}건을 동기화했습니다.", legalDongCount)
        val classificationCount = classificationCodeSyncService.sync()
        logger.info("관광 분류체계 코드 {}건을 동기화했습니다.", classificationCount)
        val tourismContentCount = tourismContentSyncService.syncGyeongsangbukdo()
        logger.info("경상북도 관광정보 {}건을 동기화했습니다.", tourismContentCount)
        /* TODO : 관리 여행 코스 동기화는 아직 구현되지 않았습니다.
        val managedCourseCount = managedTravelCourseSyncService.sync()
        logger.info("관리 여행 코스 {}건을 동기화했습니다.", managedCourseCount)*/
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LegalDongCodeStartupSync::class.java)
    }
}
