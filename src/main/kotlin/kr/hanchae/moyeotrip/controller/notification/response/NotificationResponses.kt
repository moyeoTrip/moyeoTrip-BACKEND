package kr.hanchae.moyeotrip.controller.notification.response

import kr.hanchae.moyeotrip.entity.notification.NotificationType
import java.time.LocalDateTime

data class NotificationPageResponse(
    val notifications: List<NotificationResponse>,
    val nextLastId: Long?,
    val hasNext: Boolean,
    val unreadCount: Long,
)

data class NotificationResponse(
    val notificationId: Long,
    val type: NotificationType,
    val content: String,
    val chatRoomId: Long?,
    val read: Boolean,
    val createdAt: LocalDateTime,
)

data class NotificationSettingResponse(
    val chatMessageEnabled: Boolean,
    val recruitmentDeadlineEnabled: Boolean,
    val socialActivityEnabled: Boolean,
    val marketingEnabled: Boolean,
)
