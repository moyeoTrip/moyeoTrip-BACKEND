package kr.hanchae.moyeotrip.controller.notification.request

data class UpdateNotificationSettingRequest(
    val chatMessageEnabled: Boolean,
    val recruitmentDeadlineEnabled: Boolean,
    val socialActivityEnabled: Boolean,
    val marketingEnabled: Boolean,
)
