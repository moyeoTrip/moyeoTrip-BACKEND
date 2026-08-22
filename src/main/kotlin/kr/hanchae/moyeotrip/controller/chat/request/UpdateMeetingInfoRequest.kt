package kr.hanchae.moyeotrip.controller.chat.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "채팅방 집합 정보 수정 요청")
data class UpdateMeetingInfoRequest(
    @field:Schema(description = "집합 장소 위도", example = "36.5760", nullable = true)
    @field:Min(-90)
    @field:Max(90)
    val meetingLatitude: Double? = null,
    @field:Schema(description = "집합 장소 경도", example = "128.9700", nullable = true)
    @field:Min(-180)
    @field:Max(180)
    val meetingLongitude: Double? = null,
    @field:Schema(description = "집합 장소 이름 또는 상세 안내", example = "안동역 1번 출구 앞", nullable = true)
    @field:Size(max = 500)
    val meetingDetails: String? = null,
    @field:Schema(description = "집합 일시", example = "2026-09-12T08:30:00")
    val meetingDateTime: LocalDateTime,
)
