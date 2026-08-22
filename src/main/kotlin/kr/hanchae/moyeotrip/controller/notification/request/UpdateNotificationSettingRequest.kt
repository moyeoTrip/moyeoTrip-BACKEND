package kr.hanchae.moyeotrip.controller.notification.request

import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.notification.ChatNotificationMode
import java.time.DayOfWeek
import java.time.LocalTime

@Schema(description = "내 알림 상세 설정 변경 요청")
data class UpdateNotificationSettingRequest(
    @field:Schema(description = "채팅 메시지 알림 수신 방식", example = "ALL")
    val chatNotificationMode: ChatNotificationMode,
    @field:Schema(description = "모집 마감 임박 알림 수신 여부", example = "true")
    val recruitmentDeadlineEnabled: Boolean,
    @field:Schema(description = "친구 요청·피드 반응 등 소셜 활동 알림 수신 여부", example = "true")
    val socialActivityEnabled: Boolean,
    @field:Schema(description = "이벤트·추천 등 마케팅 알림 수신 여부", example = "false")
    val marketingEnabled: Boolean,
    @field:Schema(description = "방해 금지 시간대 사용 여부", example = "true")
    val doNotDisturbEnabled: Boolean = false,
    @field:Schema(description = "방해 금지 시작 시각. 사용 시 종료 시각과 함께 설정합니다.", example = "22:30", nullable = true)
    val doNotDisturbStartTime: LocalTime? = null,
    @field:Schema(description = "방해 금지 종료 시각. 자정을 넘는 시간대도 설정할 수 있습니다.", example = "07:00", nullable = true)
    val doNotDisturbEndTime: LocalTime? = null,
    @field:Schema(description = "방해 금지를 적용할 요일 목록", example = "[\"MONDAY\", \"TUESDAY\", \"WEDNESDAY\"]")
    val doNotDisturbDays: Set<DayOfWeek> = emptySet(),
)
