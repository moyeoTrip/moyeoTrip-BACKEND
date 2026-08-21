package kr.hanchae.moyeotrip.controller.notification.request

import kr.hanchae.moyeotrip.entity.notification.ChatNotificationMode
import java.time.DayOfWeek
import java.time.LocalTime

data class UpdateNotificationSettingRequest(
    val chatNotificationMode: ChatNotificationMode,
    val recruitmentDeadlineEnabled: Boolean,
    val socialActivityEnabled: Boolean,
    val marketingEnabled: Boolean,
    val doNotDisturbEnabled: Boolean = false,
    val doNotDisturbStartTime: LocalTime? = null,
    val doNotDisturbEndTime: LocalTime? = null,
    val doNotDisturbDays: Set<DayOfWeek> = emptySet(),
)
