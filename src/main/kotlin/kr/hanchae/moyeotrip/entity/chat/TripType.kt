package kr.hanchae.moyeotrip.entity.chat

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "여행 기간 유형. DAY_TRIP=시작일 하루 안에서 시작·종료 시각을 사용, OVERNIGHT=1박 이상으로 종료 날짜를 사용",
    allowableValues = ["DAY_TRIP", "OVERNIGHT"],
)
enum class TripType {
    DAY_TRIP,
    OVERNIGHT,
}
