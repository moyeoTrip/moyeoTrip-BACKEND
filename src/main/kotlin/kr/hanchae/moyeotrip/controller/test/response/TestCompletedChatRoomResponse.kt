package kr.hanchae.moyeotrip.controller.test.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import java.time.LocalDate

@Schema(description = "QA용 채팅방 여행 완료 처리 결과")
data class TestCompletedChatRoomResponse(
    @field:Schema(description = "완료 처리한 채팅방 ID", example = "101")
    val roomId: Long,
    @field:Schema(description = "완료 처리 후 채팅방 상태. 완료 상태는 CONFIRMED와 과거 여행 날짜로 판정합니다.", example = "CONFIRMED")
    val status: ChatRoomStatus,
    @field:Schema(description = "QA용으로 조정된 여행 시작일", example = "2026-08-24", type = "string", format = "date")
    val startDate: LocalDate,
    @field:Schema(description = "QA용으로 조정된 여행 종료일. 당일 여행이면 null", nullable = true, type = "string", format = "date")
    val endDate: LocalDate?,
    @field:Schema(description = "완료 여행 조건 충족 여부", example = "true")
    val completed: Boolean,
)
