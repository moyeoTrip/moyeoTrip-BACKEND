package kr.hanchae.moyeotrip.controller.notification.response

import kr.hanchae.moyeotrip.entity.notification.ChatNotificationMode
import kr.hanchae.moyeotrip.entity.notification.NotificationType
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

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
    val chatNotificationMode: ChatNotificationMode,
    val recruitmentDeadlineEnabled: Boolean,
    val socialActivityEnabled: Boolean,
    val marketingEnabled: Boolean,
    val doNotDisturbEnabled: Boolean,
    val doNotDisturbStartTime: LocalTime?,
    val doNotDisturbEndTime: LocalTime?,
    val doNotDisturbDays: Set<DayOfWeek>,
)

data class ChatRoomNotificationSettingResponse(
    val roomId: Long,
    val enabled: Boolean,
)
