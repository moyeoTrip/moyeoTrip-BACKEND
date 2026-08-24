package kr.hanchae.moyeotrip.service.auth

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class UserWithdrawalScheduler(
    private val userService: UserService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 20 0 * * *", zone = "Asia/Seoul")
    fun deleteExpiredWithdrawnUsers() {
        val deletedCount = userService.deleteExpiredWithdrawnUsers()
        if (deletedCount > 0) {
            log.info("복구 유예 기간 30일이 지난 탈퇴 계정 {}개를 영구 삭제했습니다.", deletedCount)
        }
    }
}
