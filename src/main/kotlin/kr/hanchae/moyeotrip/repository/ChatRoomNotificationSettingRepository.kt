package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.notification.ChatRoomNotificationSetting
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRoomNotificationSettingRepository : JpaRepository<ChatRoomNotificationSetting, Long> {
    fun findByUserIdAndChatRoomId(
        userId: Long,
        chatRoomId: Long,
    ): ChatRoomNotificationSetting?
}
