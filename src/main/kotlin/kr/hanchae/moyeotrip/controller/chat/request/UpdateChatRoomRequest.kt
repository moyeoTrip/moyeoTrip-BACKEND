package kr.hanchae.moyeotrip.controller.chat.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kr.hanchae.moyeotrip.entity.chat.TripType
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "채팅방 모집글 수정 요청")
data class UpdateChatRoomRequest(
    @field:Schema(description = "채팅방 제목", example = "주말에 청송 다녀올 사람")
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,
    @field:Schema(description = "채팅방 설명. 비우면 설명을 삭제합니다.", nullable = true, maxLength = 500)
    @field:Size(max = 500)
    val description: String? = null,
    @field:Schema(description = "여행 유형", example = "DAY_TRIP")
    val tripType: TripType,
    @field:Schema(description = "최소 출발 인원", example = "3")
    @field:Min(3)
    @field:Max(20)
    val minimumParticipants: Int,
    @field:Schema(description = "최대 참가 인원", example = "5")
    @field:Min(3)
    @field:Max(20)
    val maxParticipants: Int,
    @field:Schema(description = "여행 시작일", example = "2026-09-12")
    val startDate: LocalDate,
    @field:Schema(description = "숙박 여행 종료일. 당일치기면 null", nullable = true)
    val endDate: LocalDate? = null,
    @field:Schema(description = "모집 마감일", example = "2026-09-09")
    val recruitmentDeadlineDate: LocalDate,
    @field:Schema(description = "당일 여행 시작 시각. 숙박이면 null", nullable = true)
    val dayTripStartTime: LocalTime? = null,
    @field:Schema(description = "당일 여행 종료 시각. 숙박이면 null", nullable = true)
    val dayTripEndTime: LocalTime? = null,
    @field:Schema(description = "참가비. 추후 결정이면 null", nullable = true)
    @field:Min(0)
    val participationFee: Long? = null,
)
