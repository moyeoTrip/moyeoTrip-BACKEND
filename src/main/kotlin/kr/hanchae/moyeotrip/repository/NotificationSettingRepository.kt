package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.notification.NotificationSetting
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationSettingRepository : JpaRepository<NotificationSetting, Long> {
    fun findByUserId(userId: Long): NotificationSetting?
}
