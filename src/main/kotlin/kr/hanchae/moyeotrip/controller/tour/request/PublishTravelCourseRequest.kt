package kr.hanchae.moyeotrip.controller.tour.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "완료한 커스텀 여행 코스 공개 요청")
data class PublishTravelCourseRequest(
    @field:Schema(description = "공개할 여행 코스 제목", example = "주왕산 & 주산지 힐링 트레킹")
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,
    @field:Schema(description = "공개할 여행 코스 소개", example = "가을 단풍과 호수를 함께 즐기는 하루 코스")
    @field:NotBlank
    @field:Size(max = 500)
    val description: String,
    @field:Schema(description = "공개 코스에 작성자 닉네임을 표시할지 여부", example = "true")
    val showCreatorNickname: Boolean = true,
)
